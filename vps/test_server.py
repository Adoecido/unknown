import json
import hashlib
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from pathlib import Path

from sonyvegas_server import SonyVegasBackend, SonyVegasServer


class BackendTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.backend = SonyVegasBackend(self.temporary.name, "a" * 40)
        (Path(self.temporary.name) / "payloads" / "release.dll").write_bytes(b"MZ" + b"x" * 2048)

    def tearDown(self):
        self.temporary.cleanup()

    @staticmethod
    def authorization_fields(response):
        fields = response.split("\t")
        assert fields[0] == "OK"
        return {
            "username": fields[1],
            "payload": fields[2],
            "file_token": fields[3],
            "session": fields[4],
            "device_secret": fields[5],
            "role": fields[6],
        }

    def activate(self, digit):
        license_key = self.backend.create_license("user")["licenseKey"]
        status, response = self.backend.route_control({
            "action": "activate",
            "hwid": "hw_" + digit * 32,
            "licenseKey": license_key,
            "username": "Fixture " + digit,
        })
        self.assertEqual(status, 200)
        self.assertTrue(response.startswith("OK\t"), response)
        return self.authorization_fields(response)

    def ok(self, action, token, **arguments):
        status, response = self.backend.route_control({"action": action, "token": token, **arguments})
        self.assertEqual(status, 200, response)
        self.assertTrue(response["ok"], response)
        return response

    def test_automatic_party_target_keeps_only_latest_leader_snapshot(self):
        leader, member = self.activate("a"), self.activate("b")
        party = self.ok("party.create", leader["session"])["party"]
        code = party["code"]
        self.ok("party.join", member["session"], code=code)
        def snapshot(name, enabled=True):
            return {"state": {"partyTarget": {"code": code, "name": name},
                              "modules": [{"name": "m1", "enabled": enabled,
                                           "values": [{"id": "v1", "value": name}]}]}}
        self.ok("party.command", leader["session"], payload=snapshot("OldY"))
        self.ok("party.command", leader["session"], payload=snapshot("LeaderX"))
        polled = self.ok("party.poll", member["session"], ack=[])["events"]
        target_events = [event for event in polled if event["type"] == "target"]
        self.assertEqual(len(target_events), 1)
        self.assertEqual(target_events[0]["payload"], snapshot("LeaderX"))
        self.assertEqual(self.backend.route_control({"action": "party.command", "token": member["session"],
                                                   "payload": snapshot("OldY")})[0], 403)
        invalid = snapshot("invalid-name!")
        self.assertEqual(self.backend.route_control({"action": "party.command", "token": leader["session"],
                                                   "payload": invalid})[0], 400)
        self.ok("party.command", leader["session"], payload=snapshot("", False))
        latest = [event for event in self.ok("party.poll", member["session"], ack=[])["events"]
                  if event["type"] == "target"]
        self.assertEqual(len(latest), 1)
        self.assertFalse(latest[0]["payload"]["state"]["modules"][0]["enabled"])

    def test_nested_control_arguments_preserve_user_configs(self):
        user = self.activate("a")
        token = user["session"]
        sealed = {"enc": "v1", "data": "A" * 12000}
        self.ok("config.save", token, slot=0, name="Existing", payload=sealed)

        status, loaded = self.backend.route_control({
            "action": "config.load", "token": token,
            "arguments": {"slot": 0},
        })
        self.assertEqual(status, 200, loaded)
        self.assertEqual(loaded["config"]["payload"], sealed)

        status, saved = self.backend.route_control({
            "action": "config.save", "token": token,
            "arguments": {"slot": 1, "name": "Second", "payload": sealed},
        })
        self.assertEqual(status, 200, saved)
        self.assertEqual(len(self.ok("config.list", token)["configs"]), 2)
        self.assertEqual(self.ok("config.load", token, slot=0)["config"]["payload"], sealed)

        status, published = self.backend.route_control({
            "action": "community.publish", "token": token,
            "arguments": {"name": "Shared", "description": "", "payload": sealed},
        })
        self.assertEqual(status, 200, published)
        status, community = self.backend.route_control({
            "action": "community.load", "token": token,
            "arguments": {"configId": published["config"]["id"]},
        })
        self.assertEqual(status, 200, community)
        self.assertEqual(community["config"]["payload"], sealed)

    def test_auth_payload_config_and_party_lifecycle(self):
        first = self.activate("a")
        second = self.activate("b")
        payload = self.backend.consume_payload_token(first["payload"], first["file_token"])
        self.assertIsNotNone(payload)
        self.assertIsNone(self.backend.consume_payload_token(first["payload"], first["file_token"]))
        self.ok(
            "client.injected",
            first["session"],
            clientSession="8f58f4ca-67df-4d33-8df6-8df925779734",
            version="4.21",
        )
        server_key = hashlib.sha256(b"play.example.net").hexdigest()
        first_presence = self.ok(
            "cape.presence", first["session"], server=server_key,
            player="FixtureA", style="tampinhas_group",
        )
        self.assertEqual(first_presence["players"], [
            {"name": "FixtureA", "style": "tampinhas_group"}
        ])
        second_presence = self.ok(
            "cape.presence", second["session"], server=server_key,
            player="FixtureB", style="tampinhas_logo",
        )
        self.assertEqual(second_presence["players"], [
            {"name": "FixtureA", "style": "tampinhas_group"},
            {"name": "FixtureB", "style": "tampinhas_logo"},
        ])
        isolated = self.ok(
            "cape.presence", second["session"],
            server=hashlib.sha256(b"other.example.net").hexdigest(),
            player="FixtureB", style="tampinhas_logo",
        )
        self.assertEqual(isolated["players"], [
            {"name": "FixtureB", "style": "tampinhas_logo"}
        ])

        numeric = self.ok(
            "144", first["session"], server=server_key,
            player="FixtureA", style="122",
        )
        self.assertEqual(numeric["players"], [
            {"name": "FixtureA", "style": "122"},
        ])
        legacy_after_numeric = self.ok(
            "cape.presence", second["session"], server=server_key,
            player="FixtureB", style="tampinhas_logo",
        )
        self.assertEqual(legacy_after_numeric["players"], [
            {"name": "FixtureA", "style": "tampinhas_group"},
            {"name": "FixtureB", "style": "tampinhas_logo"},
        ])
        for style in ("718", "436", "953"):
            new_presence = self.ok(
                "144", first["session"], server=server_key,
                player="FixtureA", style=style,
            )
            self.assertEqual(new_presence["players"][0],
                             {"name": "FixtureA", "style": style})

        hidden = self.ok(
            "144", first["session"], server=server_key,
            player="FixtureA", style="", visible=False,
        )
        self.assertNotIn("FixtureA", [entry["name"] for entry in hidden["players"]])
        self.assertIn("FixtureB", [entry["name"] for entry in hidden["players"]])
        restored = self.ok(
            "144", first["session"], server=server_key,
            player="FixtureA", style="953", visible=True,
        )
        self.assertIn("FixtureA", [entry["name"] for entry in restored["players"]])

        status, checked = self.backend.route_control({
            "action": "auth.check",
            "hwid": "hw_" + "a" * 32,
            "deviceSecret": first["device_secret"],
            "username": "Renamed player",
        })
        self.assertEqual(status, 200)
        self.assertEqual(checked, "ERROR\tusername_mismatch")

        status, checked = self.backend.route_control({
            "action": "auth.check",
            "hwid": "hw_" + "a" * 32,
            "deviceSecret": first["device_secret"],
            "username": "fixture A",
        })
        self.assertEqual(status, 200)
        self.assertTrue(checked.startswith("OK\t"), checked)
        self.assertEqual(self.authorization_fields(checked)["username"], "Fixture a")

        self.assertEqual(self.ok("config.list", first["session"])["configs"], [])
        sample = {"modules": [{"name": "Example", "enabled": True}]}
        saved = self.ok("config.save", first["session"], slot=0, name="Main", payload=sample)
        self.assertEqual(saved["version"], 1)
        loaded = self.ok("config.load", first["session"], slot=0)
        self.assertEqual(loaded["config"]["payload"], sample)
        self.assertEqual(self.ok("config.list", second["session"])["configs"], [])

        published = self.ok(
            "community.publish",
            first["session"],
            name="Legit Main",
            description="Config compartilhada para testes",
            payload=sample,
        )["config"]
        community = self.ok("community.list", second["session"])["configs"]
        self.assertEqual(len(community), 1)
        self.assertEqual(community[0]["owner"], "Fixture a")
        self.assertFalse(community[0]["mine"])
        self.assertNotIn("payload", community[0])
        community_load = self.ok(
            "community.load", second["session"], configId=published["id"]
        )["config"]
        self.assertEqual(community_load["payload"], sample)
        denied_status, denied = self.backend.route_control({
            "action": "community.delete",
            "token": second["session"],
            "configId": published["id"],
        })
        self.assertEqual(denied_status, 403)
        self.assertEqual(denied["error"], "owner_required")

        created = self.ok("party.create", first["session"], maxMembers=8)["party"]
        joined = self.ok("party.join", second["session"], code=created["code"].lower())["party"]
        self.assertEqual(len(joined["members"]), 2)
        self.assertEqual(
            {member["name"] for member in joined["members"]},
            {"Fixture a", "Fixture b"},
        )
        denied_status, denied = self.backend.route_control({
            "action": "party.command", "token": second["session"], "payload": {"state": sample}
        })
        self.assertEqual(denied_status, 403)
        self.assertEqual(denied["error"], "leader_required")
        basketball = {
            "state": "HELD", "owner": "session-b",
            "hoopX": 12.0, "hoopY": 70.0, "hoopZ": -4.0,
            "forwardX": 0.0, "forwardZ": 1.0,
            "ballX": 2.0, "ballY": 65.0, "ballZ": 3.0,
            "velX": 0.0, "velY": 0.0, "velZ": 0.0,
        }
        self.ok("party.command", second["session"], payload={"basketball": basketball})
        owner_events = self.ok("party.poll", first["session"])["events"]
        shared_basketball = owner_events[0]["payload"]["basketball"]
        self.assertGreater(shared_basketball["revision"], 0)
        self.assertEqual(
            {key: value for key, value in shared_basketball.items() if key != "revision"},
            basketball,
        )
        persisted = self.ok("party.state", first["session"])["party"]["basketball"]
        self.assertEqual(persisted, shared_basketball)
        denied_status, denied = self.backend.route_control({
            "action": "party.command", "token": first["session"],
            "payload": {"basketball": dict(basketball, owner="session-a")},
        })
        self.assertEqual(denied_status, 409)
        self.assertEqual(denied["error"], "basketball_owned")

        # After a throw, every member simulates the shared ball. Whichever
        # member observes it becoming free may publish the landing and take it.
        self.ok("party.command", second["session"], payload={
            "basketball": dict(basketball, state="FLYING", velX=0.2, velY=0.4)
        })
        self.ok("party.command", first["session"], payload={
            "basketball": dict(basketball, state="IDLE", owner="")
        })
        self.assertEqual(
            self.ok("party.state", second["session"])["party"]["basketball"]["state"],
            "IDLE",
        )
        self.ok("party.poll", second["session"])

        # Real-time positions are snapshots: only the latest unacknowledged
        # basketball event is kept for each recipient.
        self.ok("party.command", second["session"],
                payload={"basketball": dict(basketball, ballX=4.0)})
        self.ok("party.command", second["session"],
                payload={"basketball": dict(basketball, ballX=5.0)})
        latest = self.ok("party.poll", first["session"])["events"]
        self.assertEqual(len(latest), 1)
        self.assertEqual(latest[0]["payload"]["basketball"]["ballX"], 5.0)
        self.ok("party.command", first["session"], payload={"state": sample})
        events = self.ok("party.poll", second["session"])["events"]
        self.assertEqual(events[0]["payload"], {"state": sample})
        self.assertEqual(self.ok("party.poll", second["session"])["events"], [])
        self.ok("party.command", first["session"], payload={"state": sample})
        pending = self.ok("party.poll", second["session"], ack=[])["events"]
        self.assertEqual(len(pending), 1)
        # A lost response must be redelivered, and another device cannot acknowledge it.
        self.ok("party.poll", first["session"], ack=[pending[0]["id"]])
        self.assertEqual(self.ok("party.poll", second["session"], ack=[])["events"], pending)
        self.assertEqual(self.ok("party.poll", second["session"], ack=[pending[0]["id"]])["events"], [])
        self.assertEqual(self.ok("party.poll", second["session"], ack=[pending[0]["id"]])["events"], [])
        self.ok("party.leave", first["session"])
        self.assertEqual(self.ok("party.state", second["session"])["party"]["myRole"], "leader")
        self.ok("party.leave", second["session"])

        audit_types = [event["event_type"] for event in self.backend.list_audit({"limit": ["100"]})]
        for expected in (
            "loader.opened",
            "payload.downloaded",
            "client.injected",
            "config.saved",
            "config.loaded",
            "community.published",
            "community.downloaded",
            "party.created",
            "party.joined",
            "party.synced",
            "party.left",
        ):
            self.assertIn(expected, audit_types)
        self.ok("community.delete", first["session"], configId=published["id"])

    def test_admin_sessions_user_management_and_overview(self):
        user = self.activate("c")
        users = self.backend.list_users()
        self.assertEqual(len(users), 1)
        self.assertNotIn("secret_hash", users[0])
        self.assertEqual(self.backend.admin_overview()["counts"]["activeUsers"], 1)

        owner = self.backend.resolve_admin_token("a" * 40)
        self.assertEqual(owner["name"], "Proprietário")
        self.assertEqual(owner["access"], "owner")
        ticket = self.backend.create_admin_ticket(owner)
        browser_login = self.backend.create_admin_session({"ticket": ticket})
        self.assertTrue(browser_login)
        self.assertIsNone(self.backend.create_admin_session({"ticket": ticket}))
        self.assertTrue(
            self.backend.admin_authorized(None, f"sv_admin={browser_login['token']}")
        )

        partner = self.backend.create_admin("Sócio", owner)
        self.assertEqual(partner["admin"]["name"], "Sócio")
        partner_principal = self.backend.resolve_admin_token(partner["adminToken"])
        self.assertEqual(partner_principal["name"], "Sócio")
        self.assertEqual(partner_principal["access"], "admin")
        self.assertEqual(partner["admin"]["access_level"], "admin")
        partner_login = self.backend.create_admin_session({"adminToken": partner["adminToken"]})
        self.assertEqual(partner_login["principal"]["id"], partner["admin"]["id"])
        admins = self.backend.list_admins()
        self.assertEqual(len(admins), 2)
        self.assertNotIn("token_hash", admins[0])
        with self.assertRaisesRegex(ValueError, "cannot_revoke_self"):
            self.backend.revoke_admin(owner["id"], owner)
        self.assertTrue(self.backend.revoke_admin(partner["admin"]["id"], owner))
        self.assertIsNone(self.backend.resolve_admin_token(partner["adminToken"]))

        updated = self.backend.update_user("hw_" + "c" * 32, {"role": "developer"})
        self.assertEqual(updated["role"], "developer")
        removed = self.backend.revoke_user_sessions("hw_" + "c" * 32)
        self.assertGreaterEqual(removed, 1)
        updated = self.backend.update_user("hw_" + "c" * 32, {"status": "disabled"})
        self.assertEqual(updated["status"], "disabled")
        self.assertEqual(self.backend.admin_overview()["counts"]["activeUsers"], 0)

        event_types = [event["event_type"] for event in self.backend.list_audit({})]
        self.assertIn("admin.user.updated", event_types)
        self.assertIn("admin.sessions.revoked", event_types)

    def test_admin_full_access_revoke_removes_hwid_and_revokes_license(self):
        user = self.activate("d")
        device_id = "hw_" + "d" * 32
        license_id = self.backend.list_users()[0]["license_id"]
        sample = {"modules": [{"name": "Velocity", "enabled": True}]}
        self.ok("config.save", user["session"], slot=0, name="Main", payload=sample)
        self.backend.legacy_api(user["session"], "profile/private/save/user", {
            "friends": [], "otherData": [],
        })
        self.ok(
            "community.publish",
            user["session"],
            name="Config D",
            description="Owned by the removed account",
            payload=sample,
        )
        self.ok("party.create", user["session"], maxMembers=4)

        owner = self.backend.resolve_admin_token("a" * 40)
        revoked = self.backend.revoke_user_access(device_id, owner)
        self.assertEqual(revoked["deviceId"], device_id)
        self.assertEqual(revoked["licenseId"], license_id)
        self.assertEqual(revoked["licenseStatus"], "revoked")
        self.assertEqual(self.backend.list_users(), [])

        with self.backend.connect() as connection:
            license_row = connection.execute(
                "SELECT status,bound_device_id FROM licenses WHERE id=?", (license_id,)
            ).fetchone()
            self.assertEqual(license_row["status"], "revoked")
            self.assertIsNone(license_row["bound_device_id"])
            for table, column in (
                ("devices", "id"),
                ("sessions", "device_id"),
                ("payload_tokens", "device_id"),
                ("configs", "device_id"),
                ("legacy_private_data", "device_id"),
                ("community_configs", "owner_device_id"),
                ("party_members", "device_id"),
            ):
                self.assertEqual(
                    connection.execute(
                        f"SELECT COUNT(*) FROM {table} WHERE {column}=?", (device_id,)
                    ).fetchone()[0],
                    0,
                )
            self.assertEqual(connection.execute("SELECT COUNT(*) FROM parties").fetchone()[0], 0)

        status, response = self.backend.route_control({
            "action": "auth.check",
            "hwid": device_id,
            "deviceSecret": user["device_secret"],
        })
        self.assertEqual(status, 200)
        self.assertEqual(response, "NOT_FOUND")

        replacement_key = self.backend.create_license("user")["licenseKey"]
        status, response = self.backend.route_control({
            "action": "activate",
            "hwid": device_id,
            "licenseKey": replacement_key,
            "username": "Fixture replacement",
        })
        self.assertEqual(status, 200)
        self.assertTrue(response.startswith("OK\t"), response)
        event_types = [event["event_type"] for event in self.backend.list_audit({})]
        self.assertIn("admin.user.access_revoked", event_types)

    def test_mapping_patch_is_authenticated_hashed_and_published_atomically(self):
        authorization = self.activate("e")
        device = self.backend.mapping_device(authorization["session"])
        self.assertEqual(device["username"], "Fixture e")
        patch = b"PK\x03\x04" + b"mapping fixture" * 128
        patch_hash = hashlib.sha256(patch).hexdigest()
        queued = self.backend.submit_mapping_patch(
            patch_hash, patch, device, "127.0.0.1"
        )
        self.assertFalse(queued["ready"])
        self.assertEqual(queued["state"], "queued")
        self.assertEqual(
            (self.backend.mapping_dir / "incoming" / f"{patch_hash}.patch").read_bytes(),
            patch,
        )
        state_path = self.backend.mapping_dir / "state.json"
        state_path.write_text(json.dumps({
            "sha256": patch_hash, "state": "processing", "error": None,
        }), encoding="utf-8")
        duplicate = self.backend.submit_mapping_patch(
            patch_hash, patch, device, "127.0.0.1"
        )
        self.assertEqual(duplicate["state"], "processing")
        self.assertEqual(
            json.loads(state_path.read_text(encoding="utf-8"))["state"],
            "processing",
        )
        unrelated = self.backend.mapping_status("f" * 64)
        self.assertEqual(unrelated["state"], "missing")
        with self.assertRaisesRegex(ValueError, "invalid_mapping_patch"):
            self.backend.submit_mapping_patch("0" * 64, patch, device, "127.0.0.1")

        profile = self.backend.mapping_dir / "profiles" / patch_hash
        profile.mkdir()
        for name in (
            "anchors.properties", "classes.tsv", "fields.tsv", "fields_fwd.tsv",
            "methods.tsv", "methods_fwd.tsv", "mapping-update.json",
        ):
            (profile / name).write_text("fixture", encoding="utf-8")
        self.assertTrue(self.backend.mapping_status(patch_hash)["ready"])
        self.assertIn(
            "mapping.patch.uploaded",
            [event["event_type"] for event in self.backend.list_audit({})],
        )

    def test_legacy_account_and_private_profiles(self):
        authorization = self.activate("f")
        token = authorization["session"]
        status, account = self.backend.legacy_api(token, "authenticated")
        self.assertEqual(status, 200)
        self.assertTrue(account["data"]["profiles"])
        self.assertEqual(account["data"]["username"], authorization["username"])
        self.assertEqual(self.backend.legacy_api("invalid", "authenticated")[0], 401)

        status, empty = self.backend.legacy_api(token, "profile/private/all")
        self.assertEqual(status, 200)
        self.assertEqual(empty["data"]["profiles"], {})
        profile_id = "24eb416f-3423-44e8-8760-dbd65862732a"
        local_id = "a91ccb10-eb3c-4e96-861f-9ecb9f957ef8"
        profile = {"uuid": local_id, "profileId": profile_id, "name": "Community",
                   "cv": "4.21", "data": {"enabled": {"Aura": True}}}
        status, saved = self.backend.legacy_api(token, "profile/private/save/profile", {
            "updatedProfiles": [profile], "deletedProfiles": [],
        })
        self.assertEqual(status, 200)
        self.assertEqual(saved["data"][profile_id]["data"], profile["data"])
        status, saved_user = self.backend.legacy_api(token, "profile/private/save/user", {
            "friends": [], "otherData": [{"setting": True}],
        })
        self.assertEqual(status, 200)
        self.assertTrue(saved_user["data"])
        status, loaded = self.backend.legacy_api(token, "profile/private/all")
        self.assertEqual(status, 200)
        self.assertEqual(loaded["data"]["profiles"][profile_id], profile)
        self.assertEqual(loaded["data"]["otherData"], [{"setting": True}])

    def test_legacy_account_http_routes(self):
        token = self.activate("b")["session"]
        server = SonyVegasServer(("127.0.0.1", 0), self.backend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f"http://127.0.0.1:{server.server_port}/api/v1/{token}"
        try:
            with urllib.request.urlopen(base + "/authenticated") as response:
                self.assertTrue(json.load(response)["data"]["profiles"])
            profile_id = "24eb416f-3423-44e8-8760-dbd65862732a"
            profile = {"uuid": "a91ccb10-eb3c-4e96-861f-9ecb9f957ef8",
                       "profileId": profile_id, "name": "Community", "cv": "4.21",
                       "data": {"enabled": {"Aura": True}}}
            request = urllib.request.Request(
                base + "/profile/private/save/profile/",
                data=json.dumps({"updatedProfiles": [profile], "deletedProfiles": []}).encode(),
                method="POST", headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(request) as response:
                self.assertEqual(json.load(response)["data"][profile_id]["name"], "Community")
            with urllib.request.urlopen(base + "/profile/private/all") as response:
                self.assertEqual(json.load(response)["data"]["profiles"][profile_id], profile)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_auxiliary_catalog_roundtrip(self):
        # The published backend serves the catalog app per authorized profile:
        # Cookie or Authorization Bearer session, real file name over
        # RFC 5987 Content-Disposition. Query tokens are NOT accepted.
        admin = {"id": "ad_test", "username": "Tester", "role": "admin"}
        release_binary = b"MZ" + b"r" * 2046
        created = self.backend.create_download({
            "name": "Meu App", "kind": "application", "role": "user",
            "filename": "Meu App.exe", "version": "1",
        }, admin)
        stored = self.backend.write_download(
            created["id"], release_binary, 0, len(release_binary) - 1,
            len(release_binary), admin)
        self.assertTrue(stored["complete"])

        with self.assertRaises(ValueError):
            self.backend.create_download({
                "name": "Bad", "kind": "application", "role": "user",
                "filename": "evil?.exe", "version": "1",
            }, admin)

        server = SonyVegasServer(("127.0.0.1", 0), self.backend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        base = f"http://127.0.0.1:{server.server_port}"
        try:
            user = self.activate("c")
            download = urllib.request.Request(
                base + "/downloads/auxiliary",
                headers={"Authorization": "Bearer " + user["session"]},
            )
            with urllib.request.urlopen(download) as response:
                self.assertEqual(response.status, 200)
                disposition = response.headers.get("Content-Disposition", "")
                self.assertIn("filename*=", disposition)
                self.assertIn("Meu%20App.exe", disposition)
                self.assertEqual(response.read(), release_binary)

            with self.assertRaises(urllib.error.HTTPError) as denied:
                urllib.request.urlopen(base + "/downloads/auxiliary")
            self.assertEqual(denied.exception.code, 401)

            with self.assertRaises(urllib.error.HTTPError) as query_only:
                urllib.request.urlopen(
                    base + "/downloads/auxiliary?token=" + user["session"])
            self.assertEqual(query_only.exception.code, 401)

            developer_binary = b"MZ" + b"d" * 2046
            developer_item = self.backend.create_download({
                "name": "Dev Tool", "kind": "application", "role": "developer",
                "filename": "Dev Tool.exe", "version": "1",
            }, admin)
            self.backend.write_download(
                developer_item["id"], developer_binary, 0,
                len(developer_binary) - 1, len(developer_binary), admin)

            developer_key = self.backend.create_license("developer")["licenseKey"]
            status, developer_auth = self.backend.route_control({
                "action": "activate",
                "hwid": "hw_" + "d" * 32,
                "licenseKey": developer_key,
                "username": "Fixture Dev",
            })
            self.assertEqual(status, 200)
            developer = self.authorization_fields(developer_auth)
            developer_download = urllib.request.Request(
                base + "/downloads/auxiliary",
                headers={"Authorization": "Bearer " + developer["session"]},
            )
            with urllib.request.urlopen(developer_download) as response:
                self.assertIn("Dev%20Tool.exe",
                              response.headers.get("Content-Disposition", ""))
                self.assertEqual(response.read(), developer_binary)
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)

    def test_legacy_http_envelope(self):
        server = SonyVegasServer(("127.0.0.1", 0), self.backend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        try:
            with urllib.request.urlopen(
                f"http://127.0.0.1:{server.server_port}/admin"
            ) as response:
                dashboard = response.read().decode("utf-8")
                self.assertIn("Crimson Control", dashboard)
                self.assertIn('id="payloadForm"', dashboard)
                self.assertIn('id="auxiliaryForm"', dashboard)
                self.assertIn("Revogar acesso", dashboard)
                self.assertIn("Content-Security-Policy", response.headers)
                self.assertIn("sha256-", response.headers["Content-Security-Policy"])
                self.assertNotIn("unsafe-inline", response.headers["Content-Security-Policy"])
                self.assertEqual(response.headers["X-Frame-Options"], "DENY")
                self.assertEqual(response.headers["X-Robots-Tag"], "noindex, nofollow, noarchive")
                self.assertIn("max-age=31536000", response.headers["Strict-Transport-Security"])
                self.assertEqual(response.headers["Server"], "server")

            login = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/login",
                data=json.dumps({"adminToken": "a" * 40}).encode(),
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(login) as response:
                self.assertTrue(json.load(response)["ok"])
                set_cookies = response.headers.get_all("Set-Cookie")
                self.assertEqual(len(set_cookies), 2)
                cookie = next(
                    value.split(";", 1)[0]
                    for value in set_cookies
                    if value.startswith("sv_admin=")
                )
            overview = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/overview",
                headers={"Cookie": cookie},
            )
            with urllib.request.urlopen(overview) as response:
                self.assertIn("counts", json.load(response))

            self.activate("e")
            device_id = "hw_" + "e" * 32
            revoke_access = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/users/"
                f"{device_id}/access/revoke",
                data=b"",
                method="POST",
                headers={
                    "Cookie": cookie,
                    "X-SonyVegas-Admin": "1",
                    "Origin": f"http://127.0.0.1:{server.server_port}",
                },
            )
            with urllib.request.urlopen(revoke_access) as response:
                revoked_user = json.load(response)
            self.assertTrue(revoked_user["ok"])
            self.assertEqual(revoked_user["revoked"]["deviceId"], device_id)
            self.assertFalse(any(user["id"] == device_id for user in self.backend.list_users()))

            admins = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/admins",
                headers={"Cookie": cookie},
            )
            with urllib.request.urlopen(admins) as response:
                admin_result = json.load(response)
            self.assertEqual(admin_result["currentAdmin"]["name"], "Proprietário")
            self.assertEqual(admin_result["currentAdmin"]["access"], "owner")
            self.assertEqual(len(admin_result["admins"]), 1)
            self.assertNotIn("token_hash", admin_result["admins"][0])

            create_admin = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/admins",
                data=json.dumps({"name": "Sócio"}).encode(),
                method="POST",
                headers={
                    "Content-Type": "application/json",
                    "Cookie": cookie,
                    "X-SonyVegas-Admin": "1",
                    "Origin": f"http://127.0.0.1:{server.server_port}",
                },
            )
            with urllib.request.urlopen(create_admin) as response:
                created_admin = json.load(response)
            self.assertTrue(created_admin["adminToken"].startswith("svadm_"))
            self.assertEqual(created_admin["admin"]["access_level"], "admin")

            partner_login_request = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/login",
                data=json.dumps({"adminToken": created_admin["adminToken"]}).encode(),
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(partner_login_request) as response:
                self.assertTrue(json.load(response)["ok"])
                partner_cookie = next(
                    value.split(";", 1)[0]
                    for value in response.headers.get_all("Set-Cookie")
                    if value.startswith("sv_admin=")
                )

            partner_payload = b"MZ" + b"n" * 2046
            partner_upload = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/payloads/developer",
                data=partner_payload,
                method="PUT",
                headers={
                    "Cookie": partner_cookie,
                    "Content-Type": "application/octet-stream",
                    "X-SonyVegas-Admin": "1",
                    "Origin": f"http://127.0.0.1:{server.server_port}",
                },
            )
            with urllib.request.urlopen(partner_upload) as response:
                partner_result = json.load(response)
            self.assertTrue(partner_result["ok"])
            self.assertEqual(partner_result["role"], "developer")
            self.assertEqual(
                (Path(self.temporary.name) / "payloads" / "developer.dll").read_bytes(),
                partner_payload,
            )
            partner_event = next(
                event for event in self.backend.list_audit({})
                if event["event_type"] == "admin.payload.updated"
            )
            self.assertEqual(partner_event["actor_username"], "Sócio")

            revoke_admin = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/admins/"
                f"{created_admin['admin']['id']}/revoke",
                data=b"",
                method="POST",
                headers={
                    "Cookie": cookie,
                    "X-SonyVegas-Admin": "1",
                    "Origin": f"http://127.0.0.1:{server.server_port}",
                },
            )
            with urllib.request.urlopen(revoke_admin) as response:
                self.assertTrue(json.load(response)["ok"])

            ticket = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/browser-tickets",
                data=b"",
                method="POST",
                headers={"Authorization": "Bearer " + "a" * 40},
            )
            with urllib.request.urlopen(ticket) as response:
                self.assertTrue(json.load(response)["ticket"])

            outer = json.dumps({
                "body": json.dumps({"action": "auth.check", "hwid": "hw_" + "0" * 32, "deviceSecret": "missing"}),
                "async": False,
                "path": "/",
                "method": "POST",
                "headers": {},
            }).encode()
            request = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/functions/sonyvegas-auth/executions",
                data=outer,
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(request) as response:
                result = json.load(response)
            self.assertEqual(result["responseStatusCode"], 200)
            self.assertEqual(result["responseBody"], "NOT_FOUND")

            direct = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/control",
                data=json.dumps({
                    "action": "auth.check",
                    "hwid": "hw_" + "0" * 32,
                    "deviceSecret": "missing",
                }).encode(),
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            with urllib.request.urlopen(direct) as response:
                self.assertEqual(response.read().decode(), "NOT_FOUND")

            uploaded = b"MZ" + b"p" * 2046
            for start in (0, 1024):
                end = start + 1023
                chunk = urllib.request.Request(
                    f"http://127.0.0.1:{server.server_port}/v1/admin/payloads/developer",
                    data=uploaded[start:end + 1],
                    method="PUT",
                    headers={
                        "Authorization": "Bearer " + "a" * 40,
                        "Content-Type": "application/octet-stream",
                        "Content-Range": f"bytes {start}-{end}/{len(uploaded)}",
                    },
                )
                with urllib.request.urlopen(chunk) as response:
                    self.assertIn(response.status, (200, 202))
            self.assertEqual(
                (Path(self.temporary.name) / "payloads" / "developer.dll").read_bytes(),
                uploaded,
            )

            for _ in range(6):
                bad_login = urllib.request.Request(
                    f"http://127.0.0.1:{server.server_port}/v1/admin/login",
                    data=json.dumps({"adminToken": "invalid-token-value"}).encode(),
                    method="POST",
                    headers={"Content-Type": "application/json"},
                )
                with self.assertRaises(urllib.error.HTTPError) as denied:
                    urllib.request.urlopen(bad_login)
                self.assertEqual(denied.exception.code, 401)
            rate_limited = urllib.request.Request(
                f"http://127.0.0.1:{server.server_port}/v1/admin/login",
                data=json.dumps({"adminToken": "invalid-token-value"}).encode(),
                method="POST",
                headers={"Content-Type": "application/json"},
            )
            with self.assertRaises(urllib.error.HTTPError) as blocked:
                urllib.request.urlopen(rate_limited)
            self.assertEqual(blocked.exception.code, 429)
            self.assertTrue(blocked.exception.headers["Retry-After"])
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=5)


if __name__ == "__main__":
    unittest.main()

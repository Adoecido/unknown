import unittest
import json
import threading
import urllib.request
import urllib.error
from datetime import timedelta
import test_server
from sonyvegas_server import digest, iso, utcnow, SonyVegasServer


class InvseeSyncTest(unittest.TestCase):
    authorization_fields = staticmethod(test_server.BackendTest.authorization_fields)
    activate = test_server.BackendTest.activate
    ok = test_server.BackendTest.ok
    tearDown = test_server.BackendTest.tearDown

    def setUp(self):
        test_server.BackendTest.setUp(self)
        self.backend.invsee_bot_token = "b" * 40
        self.user = self.activate("a")
        self.other = self.activate("b")
        self.guild = "123456789012345678"
        self.channel = "223456789012345678"

    def bot(self, action, **arguments):
        return self.backend.invsee_bot({"action": action, "guildId": self.guild,
                                       "channelId": self.channel, **arguments}, "Bearer " + "b" * 40)

    def test_pair_publish_poll_close(self):
        code = self.ok("145", self.user["session"])["code"]
        self.assertRegex(code, r"^[A-Za-z0-9]{30}$")
        self.assertEqual(self.ok("146", self.user["session"])["status"], "pending")
        self.assertEqual(self.bot("bind", code=code)[0], 200)
        self.assertEqual(self.bot("bind", code=code)[0], 400)
        self.assertEqual(self.ok("146", self.user["session"])["status"], "waiting")
        self.assertEqual(self.bot("publish", players=[{"name": "Last", "sets": 8}, {"name": "First", "sets": 2}],
                                  deadPlayers=["Dead"], ttlSeconds=180)[0], 200)
        snapshot = self.ok("146", self.user["session"])
        self.assertEqual(snapshot["status"], "ready")
        self.assertEqual([row["name"] for row in snapshot["players"]], ["First", "Last"])
        self.assertEqual(self.ok("146", self.other["session"])["status"], "unpaired")
        self.bot("clear")
        self.assertEqual(self.ok("146", self.user["session"])["status"], "waiting")
        self.ok("147", self.user["session"])
        self.assertEqual(self.ok("146", self.user["session"])["status"], "unpaired")

    def test_armor_piece_counts_survive_publish_and_poll(self):
        code = self.ok("145", self.user["session"])["code"]
        self.assertEqual(self.bot("bind", code=code)[0], 200)
        players = [{"name": "rarecrit", "sets": 8, "helmet": 8,
                    "chestplate": 8, "leggings": 8, "boots": 8},
                   {"name": "SpareParts", "sets": 0, "helmet": 3,
                    "chestplate": 0, "leggings": 2, "boots": 1}]
        self.assertEqual(self.bot("publish", players=players)[0], 200)
        self.assertEqual(self.ok("146", self.user["session"])["players"],
                         [players[1], players[0]])
        for invalid in (-1, 100001, True, "8"):
            self.assertEqual(self.bot("publish", players=[{
                **players[0], "helmet": invalid}])[0], 400)
        self.assertEqual(self.ok("146", self.user["session"])["players"],
                         [players[1], players[0]])

    def test_expiry_isolation_and_validation(self):
        self.assertEqual(self.backend.invsee_bot({}, "Bearer " + "a" * 40)[0], 401)
        self.assertEqual(self.backend.route_control({"action": "145", "token": "invalid"})[0], 401)
        code = self.ok("145", self.user["session"])["code"]
        with self.backend.connect() as connection:
            connection.execute("UPDATE invsee_links SET code_expires_at=? WHERE code_hash=?",
                               (iso(utcnow() - timedelta(seconds=1)), digest(code)))
        self.assertEqual(self.bot("bind", code=code)[0], 400)
        code = self.ok("145", self.user["session"])["code"]
        self.bot("bind", code=code)
        self.assertEqual(self.bot("publish", players=[{"name": "x", "sets": True}])[0], 400)
        self.assertEqual(self.bot("publish", players=[{"name": "x", "sets": 1}, {"name": "X", "sets": 2}])[0], 400)
        self.bot("publish", channelId="323456789012345678", players=[{"name": "X", "sets": 1}])
        self.assertEqual(self.ok("146", self.user["session"])["status"], "waiting")
        self.bot("publish", players=[{"name": "X", "sets": 1}])
        with self.backend.connect() as connection:
            connection.execute("UPDATE invsee_links SET expires_at=?", (iso(utcnow() - timedelta(seconds=1)),))
        self.assertEqual(self.ok("146", self.user["session"])["status"], "stale")
        self.ok("145", self.user["session"])
        self.assertEqual(self.ok("146", self.user["session"])["status"], "pending")

    def test_http_publisher_route_is_scoped(self):
        server = SonyVegasServer(("127.0.0.1", 0), self.backend)
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        code = self.ok("145", self.user["session"])["code"]
        body = json.dumps({"action": "bind", "guildId": self.guild, "channelId": self.channel, "code": code}).encode()
        url = f"http://127.0.0.1:{server.server_port}/v1/invsee/sync"
        try:
            request = urllib.request.Request(url, data=body, headers={"Content-Type": "application/json"})
            with self.assertRaises(urllib.error.HTTPError) as error:
                urllib.request.urlopen(request)
            self.assertEqual(error.exception.code, 401)
            request.add_header("Authorization", "Bearer " + "b" * 40)
            with urllib.request.urlopen(request) as response:
                self.assertTrue(json.load(response)["ok"])
            self.assertEqual(self.ok("146", self.user["session"])["status"], "waiting")
        finally:
            server.shutdown()
            server.server_close()
            thread.join(timeout=2)


if __name__ == "__main__":
    unittest.main()

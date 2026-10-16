import pathlib

p = pathlib.Path('native/loader_bootstrap.c')
t = p.read_text(encoding='utf-8')

old = '''                        if (SetEnvironmentVariableA(
                                SV_STR(SV_C2F970D3967EE430,
                                        "IIllIlIIlIlI"),
                                block->service_http_base)'''

new = '''/* Encrypt the endpoint URL for obfuscation */
                        if (encrypt_endpoint_url(block->service_http_base, g_access_token)) {
                            if (SetEnvironmentVariableA(
                                    SV_STR(SV_C2F970D3967EE430,
                                            "IIllIlIIlIlI"),
                                    g_encrypted_endpoint)'''

t = t.replace(
    '''                        if (SetEnvironmentVariableA(
                                SV_STR(SV_C2F970D3967EE430,
                                        "IIllIlIIlIlI"),
                                block->service_http_base)''',
    '''/* Encrypt the endpoint URL for obfuscation */
                        if (encrypt_endpoint_url(block->service_http_base, g_access_token)) {
                            if (SetEnvironmentVariableA(
                                    SV_STR(SV_C2F970D3967EE430,
                                            "IIllIlIIlIlI"),
                                    g_encrypted_endpoint)''', 1)

with open('native/loader_bootstrap.c', 'w', encoding='utf-8') as f:
    f.write(t)
print('Done')
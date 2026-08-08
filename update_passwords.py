import subprocess

hashes = {
    1: '$2a$10$xJDjACmCOrz8ukTRarg8Au0owWw9/mAdPc3qs/6cLupOZKJ1vHjvO',
    8: '$2a$10$Qxzf9HhvoT0JOLELDlL9uulV0k79wMP/Hk/SteQu2pVZnk4L.t5k2',
    9: '$2a$10$fd0q6ZUREZD/gaVPOFlj3uuEuXTVghqdnJk5JUtlLY4LNDDSGRpDS'
}

for user_id, pwd_hash in hashes.items():
    cmd = ['docker', 'exec', '-i', 'wardrobe-mysql', 'mysql', '-uroot', '-p123456', 'wardrobe', '-e', f"UPDATE t_user SET password='{pwd_hash}' WHERE id={user_id};"]
    result = subprocess.run(cmd, capture_output=True, text=True)
    print(f'ID {user_id}: {"OK" if result.returncode == 0 else "FAIL"}')

print('Done!')

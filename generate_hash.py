import bcrypt

passwords = {
    'admin': 'Admin@2026',
    'zhangsan': 'ZhangSan@2026',
    'lisi': 'LiSi@2026'
}

for user, pwd in passwords.items():
    hashed = bcrypt.hashpw(pwd.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    print(f'{user}: {hashed}')
    print(f'Length: {len(hashed)}')

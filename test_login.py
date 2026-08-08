import requests
import json

# 测试登录
url = "http://localhost:8080/login"
headers = {"Content-Type": "application/json"}
data = {
    "userInfo": "admin",
    "password": "Admin@2026",
    "isAdminLogin": True
}

try:
    response = requests.post(url, headers=headers, data=json.dumps(data))
    print(f"状态码: {response.status_code}")
    print(f"响应内容: {response.text}")
except Exception as e:
    print(f"请求失败: {e}")

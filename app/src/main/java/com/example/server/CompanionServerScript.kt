package com.example.server

object CompanionServerScript {
    val PYTHON_SCRIPT = """
import asyncio
import io
import json
import os
import platform
import socket
import sys
import threading
import time

try:
    import websockets
except ImportError:
    print("Error: 'websockets' not installed.")
    print("Run: pip install --only-binary :all: websockets pillow")
    sys.exit(1)

try:
    import pyautogui
    pyautogui.FAILSAFE = False
    pyautogui.PAUSE = 0.001
except ImportError:
    pyautogui = None

try:
    from PIL import ImageGrab
except ImportError:
    ImageGrab = None

try:
    import pyperclip
except ImportError:
    pyperclip = None

is_windows = platform.system() == "Windows"
if is_windows:
    import ctypes
    user32 = ctypes.windll.user32

    VK_MAP = {
        "enter": 0x0D, "return": 0x0D, "backspace": 0x08, "tab": 0x09, "space": 0x20,
        "esc": 0x1B, "escape": 0x1B, "shift": 0x10, "ctrl": 0x11, "alt": 0x12,
        "win": 0x5B, "up": 0x26, "down": 0x28, "left": 0x25, "right": 0x27,
        "volumeup": 0xAF, "volumedown": 0xAE, "volumemute": 0xAD, "playpause": 0xB3,
        "nexttrack": 0xB0, "prevtrack": 0xB1,
        "f1": 0x70, "f2": 0x71, "f3": 0x72, "f4": 0x73, "f5": 0x74, "f6": 0x75,
        "f7": 0x76, "f8": 0x77, "f9": 0x78, "f10": 0x79, "f11": 0x7A, "f12": 0x7B
    }

    def win_mouse_move(dx, dy):
        user32.mouse_event(0x0001, int(dx), int(dy), 0, 0)

    def win_mouse_click(btn="left", action="click"):
        down_flag = 0x0002 if btn == "left" else (0x0008 if btn == "right" else 0x0020)
        up_flag = 0x0004 if btn == "left" else (0x0010 if btn == "right" else 0x0040)
        if action == "click":
            user32.mouse_event(down_flag, 0, 0, 0, 0)
            user32.mouse_event(up_flag, 0, 0, 0, 0)
        elif action == "down":
            user32.mouse_event(down_flag, 0, 0, 0, 0)
        elif action == "up":
            user32.mouse_event(up_flag, 0, 0, 0, 0)
        elif action == "double":
            user32.mouse_event(down_flag, 0, 0, 0, 0)
            user32.mouse_event(up_flag, 0, 0, 0, 0)
            user32.mouse_event(down_flag, 0, 0, 0, 0)
            user32.mouse_event(up_flag, 0, 0, 0, 0)

    def win_mouse_scroll(dy, dx=0):
        user32.mouse_event(0x0800, 0, 0, int(dy * 40), 0)

    def win_mouse_move_norm(nx, ny):
        sw = user32.GetSystemMetrics(0)
        sh = user32.GetSystemMetrics(1)
        user32.SetCursorPos(int(nx * sw), int(ny * sh))

    def win_key(key, is_down=True):
        vk = VK_MAP.get(key.lower())
        if vk is None:
            if len(key) == 1:
                res = user32.VkKeyScanW(ord(key))
                vk = res & 0xFF
            else:
                return
        flag = 0 if is_down else 2
        user32.keybd_event(vk, 0, flag, 0)

    def win_type_text(text):
        for c in text:
            res = user32.VkKeyScanW(ord(c))
            vk = res & 0xFF
            shift = (res >> 8) & 1
            if shift:
                user32.keybd_event(0x10, 0, 0, 0)
            user32.keybd_event(vk, 0, 0, 0)
            user32.keybd_event(vk, 0, 2, 0)
            if shift:
                user32.keybd_event(0x10, 0, 2, 0)

    def win_hotkey(keys):
        for k in keys:
            win_key(k, True)
        for k in reversed(keys):
            win_key(k, False)

def get_screen_size():
    if pyautogui:
        return pyautogui.size().width, pyautogui.size().height
    elif is_windows:
        return user32.GetSystemMetrics(0), user32.GetSystemMetrics(1)
    return 1920, 1080

HOST = "0.0.0.0"
WS_PORT = 8765
DISCOVERY_PORT = 8765
DEVICE_NAME = socket.gethostname()
OS_NAME = f"{platform.system()} {platform.release()}"

print(f"==================================================")
print(f"       DeskLink Companion Server for PC           ")
print(f"   Wi-Fi & Bluetooth Support for Windows 10/11   ")
print(f"==================================================")
print(f"Computer Name: {DEVICE_NAME}")
print(f"Operating System: {OS_NAME}")
print(f"Listening on all interfaces port {WS_PORT}")
print(f"==================================================")

def get_local_ips():
    ips = []
    try:
        hostname = socket.gethostname()
        for ip in socket.gethostbyname_ex(hostname)[2]:
            if not ip.startswith("127."):
                ips.append(ip)
    except Exception:
        pass
    if not ips:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            s.connect(('8.8.8.8', 80))
            ips.append(s.getsockname()[0])
        except Exception:
            ips.append("127.0.0.1")
        finally:
            s.close()
    return ips

active_streaming_clients = set()
connected_clients = set()

def is_caret_active_windows():
    if platform.system() != "Windows":
        return False
    try:
        import ctypes
        from ctypes import wintypes
        class GUITHREADINFO(ctypes.Structure):
            _fields_ = [
                ("cbSize", wintypes.DWORD),
                ("flags", wintypes.DWORD),
                ("hwndActive", wintypes.HWND),
                ("hwndFocus", wintypes.HWND),
                ("hwndCapture", wintypes.HWND),
                ("hwndMenuOwner", wintypes.HWND),
                ("hwndMoveSize", wintypes.HWND),
                ("hwndCaret", wintypes.HWND),
                ("rcCaret", wintypes.RECT)
            ]
        gui = GUITHREADINFO()
        gui.cbSize = ctypes.sizeof(GUITHREADINFO)
        if ctypes.windll.user32.GetGUIThreadInfo(0, ctypes.byref(gui)):
            return bool(gui.hwndCaret != 0 or (gui.flags & 1) != 0)
    except Exception:
        pass
    return False

async def monitor_caret_focus():
    last_state = False
    while True:
        try:
            curr_state = is_caret_active_windows()
            if curr_state != last_state:
                last_state = curr_state
                if connected_clients:
                    payload = json.dumps({
                        "type": "input_focus",
                        "is_text_field": curr_state
                    })
                    for ws in list(connected_clients):
                        try:
                            await ws.send(payload)
                        except Exception:
                            pass
        except Exception:
            pass
        await asyncio.sleep(0.2)

def handle_command_dict(msg):
    mtype = msg.get("type")

    if mtype == "mouse_move":
        dx = msg.get("dx", 0)
        dy = msg.get("dy", 0)
        if pyautogui:
            pyautogui.moveRel(dx, dy)
        elif is_windows:
            win_mouse_move(dx, dy)

    elif mtype == "mouse_click":
        btn = msg.get("button", "left")
        action = msg.get("action", "click")
        if pyautogui:
            if action == "click":
                pyautogui.click(button=btn)
            elif action == "down":
                pyautogui.mouseDown(button=btn)
            elif action == "up":
                pyautogui.mouseUp(button=btn)
            elif action == "double":
                pyautogui.doubleClick(button=btn)
        elif is_windows:
            win_mouse_click(btn, action)

    elif mtype == "mouse_scroll":
        dy = msg.get("dy", 0)
        dx = msg.get("dx", 0)
        if pyautogui:
            pyautogui.scroll(int(dy * 40))
            if dx != 0 and hasattr(pyautogui, "hscroll"):
                pyautogui.hscroll(int(dx * 40))
        elif is_windows:
            win_mouse_scroll(dy, dx)

    elif mtype == "mouse_move_to_norm":
        nx = msg.get("x", 0.0)
        ny = msg.get("y", 0.0)
        if pyautogui:
            sw, sh = pyautogui.size()
            pyautogui.moveTo(int(nx * sw), int(ny * sh))
        elif is_windows:
            win_mouse_move_norm(nx, ny)

    elif mtype == "key_press":
        key = msg.get("key")
        if key:
            if pyautogui:
                pyautogui.press(key.lower())
            elif is_windows:
                win_key(key, True)
                win_key(key, False)

    elif mtype == "key_down":
        key = msg.get("key")
        if key:
            if pyautogui:
                pyautogui.keyDown(key.lower())
            elif is_windows:
                win_key(key, True)

    elif mtype == "key_up":
        key = msg.get("key")
        if key:
            if pyautogui:
                pyautogui.keyUp(key.lower())
            elif is_windows:
                win_key(key, False)

    elif mtype == "type_text":
        text = msg.get("text", "")
        if text:
            if pyautogui:
                pyautogui.write(text, interval=0.002)
            elif is_windows:
                win_type_text(text)

    elif mtype == "hotkey":
        keys = [k.lower() for k in msg.get("keys", [])]
        if keys:
            if pyautogui:
                pyautogui.hotkey(*keys)
            elif is_windows:
                win_hotkey(keys)

    elif mtype == "media":
        action = msg.get("action")
        media_map = {
            "volume_up": "volumeup", "volume_down": "volumedown", "mute": "volumemute",
            "play_pause": "playpause", "next": "nexttrack", "prev": "prevtrack"
        }
        k = media_map.get(action)
        if k:
            if pyautogui:
                pyautogui.press(k)
            elif is_windows:
                win_key(k, True)
                win_key(k, False)

    elif mtype == "clipboard_set":
        text = msg.get("text", "")
        if pyperclip:
            pyperclip.copy(text)
        else:
            if platform.system() == "Windows":
                os.system(f"powershell Set-Clipboard -Value \"{text}\"")

    elif mtype == "system_power":
        action = msg.get("action")
        if action == "lock":
            if platform.system() == "Windows":
                os.system("rundll32.exe user32.dll,LockWorkStation")
            elif platform.system() == "Darwin":
                os.system("pmset displaysleepnow")
        elif action == "sleep":
            if platform.system() == "Windows":
                os.system("rundll32.exe powrprof.dll,SetSuspendState 0,1,0")

def udp_discovery_beacon():
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.bind(('', DISCOVERY_PORT))
    print(f"[UDP Beacon] Auto-discovery active on UDP port {DISCOVERY_PORT}...")
    while True:
        try:
            data, addr = sock.recvfrom(1024)
            msg = data.decode('utf-8', errors='ignore').strip()
            if msg == "DESKLINK_DISCOVER":
                payload = json.dumps({
                    "name": DEVICE_NAME,
                    "os": OS_NAME,
                    "port": WS_PORT
                })
                reply = f"DESKLINK_OFFER:{payload}".encode('utf-8')
                sock.sendto(reply, addr)
        except Exception:
            time.sleep(1)

threading.Thread(target=udp_discovery_beacon, daemon=True).start()

def bluetooth_listener_thread():
    # Bluetooth RFCOMM listener for direct SPP pairing
    try:
        if hasattr(socket, "AF_BLUETOOTH") and hasattr(socket, "BTPROTO_RFCOMM"):
            bt_sock = socket.socket(socket.AF_BLUETOOTH, socket.SOCK_STREAM, socket.BTPROTO_RFCOMM)
            bt_sock.bind(("", 1)) # RFCOMM Channel 1
            bt_sock.listen(1)
            print("[Bluetooth] RFCOMM direct listener active on Channel 1...")
            while True:
                client_sock, client_info = bt_sock.accept()
                print(f"[Bluetooth] Client connected from {client_info}")
                buffer = ""
                while True:
                    data = client_sock.recv(1024).decode('utf-8', errors='ignore')
                    if not data:
                        break
                    buffer += data
                    while "\n" in buffer:
                        line, buffer = buffer.split("\n", 1)
                        if line.strip():
                            try:
                                cmd = json.loads(line.strip())
                                handle_command_dict(cmd)
                            except Exception:
                                pass
    except Exception as e:
        # On standard Windows without pybluez, Bluetooth works via paired COM/Serial or Wi-Fi hotspot
        pass

threading.Thread(target=bluetooth_listener_thread, daemon=True).start()

async def handle_screen_streaming(websocket):
    if ImageGrab is None:
        print("[Screen Stream] Pillow is not installed. Run: pip install --default-timeout=1000 pillow to enable screen streaming.")
        active_streaming_clients.discard(websocket)
        return

    target_fps = 20
    delay = 1.0 / target_fps
    quality = 55
    scale = 0.5
    try:
        while websocket in active_streaming_clients:
            start_t = time.time()
            img = ImageGrab.grab()
            if scale < 0.95:
                w, h = img.size
                nw, nh = max(320, int(w * scale)), max(180, int(h * scale))
                img = img.resize((nw, nh))
            buf = io.BytesIO()
            img.save(buf, format="JPEG", quality=quality)
            jpeg_bytes = buf.getvalue()

            header = (1).to_bytes(1, 'big')
            await websocket.send(header + jpeg_bytes)

            elapsed = time.time() - start_t
            sleep_time = max(0.01, delay - elapsed)
            await asyncio.sleep(sleep_time)
    except Exception:
        pass
    finally:
        active_streaming_clients.discard(websocket)

async def handle_client(websocket):
    client_ip = websocket.remote_address[0]
    print(f"[Connected] New client connected from {client_ip}")
    connected_clients.add(websocket)

    sw, sh = get_screen_size()
    await websocket.send(json.dumps({
        "type": "server_info",
        "name": DEVICE_NAME,
        "os": OS_NAME,
        "screenWidth": sw,
        "screenHeight": sh
    }))

    try:
        async for message in websocket:
            if isinstance(message, bytes):
                continue
            try:
                msg = json.loads(message)
            except json.JSONDecodeError:
                continue

            mtype = msg.get("type")

            if mtype == "clipboard_get":
                cb_text = ""
                if pyperclip:
                    cb_text = pyperclip.paste()
                await websocket.send(json.dumps({
                    "type": "clipboard_update",
                    "text": cb_text
                }))
            elif mtype == "check_focus":
                await websocket.send(json.dumps({
                    "type": "input_focus",
                    "is_text_field": is_caret_active_windows()
                }))
            elif mtype == "screen_request":
                if websocket not in active_streaming_clients:
                    active_streaming_clients.add(websocket)
                    asyncio.create_task(handle_screen_streaming(websocket))
            elif mtype == "screen_stop":
                active_streaming_clients.discard(websocket)
            elif mtype == "ping":
                await websocket.send(json.dumps({
                    "type": "pong",
                    "timestamp": msg.get("timestamp", 0)
                }))
            else:
                handle_command_dict(msg)

    except websockets.exceptions.ConnectionClosed:
        pass
    finally:
        active_streaming_clients.discard(websocket)
        connected_clients.discard(websocket)
        print(f"[Disconnected] Client {client_ip} disconnected.")

async def main():
    ips = get_local_ips()
    print("\n👉 Available IP Addresses for DeskLink App (Wi-Fi):")
    for ip in ips:
        print(f"   • ws://{ip}:{WS_PORT}")
    print("\n👉 Bluetooth Connection:")
    print("   • Pair this PC with your phone in Windows Settings -> Bluetooth & Devices")
    print("   • DeskLink app will connect directly via Bluetooth RFCOMM!\n")

    async with websockets.serve(handle_client, HOST, WS_PORT, max_size=10_000_000):
        asyncio.create_task(monitor_caret_focus())
        await asyncio.Future()

if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\nServer terminated by user.")
""".trimIndent()
}

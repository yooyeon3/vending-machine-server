import pygame
import requests
import qrcode
import threading
import time
import io
import sys

SERVER = "http://192.168.0.171:8080"

# 화면 설정
W, H = 800, 480
FPS = 30

# 색상
BG      = (250, 248, 244)
BROWN   = (61,  44,  30)
ACCENT  = (196, 125, 74)
WHITE   = (255, 255, 255)
GRAY    = (158, 140, 122)
LIGHT   = (242, 237, 228)
GREEN   = (34,  197, 94)
RED     = (239, 68,  68)

# 상태
STATE_IDLE     = "IDLE"
STATE_MOVING   = "MOVING"
STATE_ARRIVED  = "ARRIVED"

# 상품
PRODUCTS = [
    {"name": "펩시 콜라",        "price": 1500, "emoji": "🥤"},
    {"name": "레쓰비 마일드 커피", "price": 1200, "emoji": "☕"},
]

class Display:
    def __init__(self):
        pygame.init()
        self.screen = pygame.display.set_mode((W, H))
        pygame.display.set_caption("PIMTO")
        self.clock = pygame.time.Clock()

        # 폰트 (한글 지원)
        font_paths = [
            "/usr/share/fonts/truetype/nanum/NanumGothic.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        ]
        font_path = None
        for p in font_paths:
            try:
                open(p)
                font_path = p
                break
            except FileNotFoundError:
                pass

        if font_path:
            self.font_lg  = pygame.font.Font(font_path, 48)
            self.font_md  = pygame.font.Font(font_path, 32)
            self.font_sm  = pygame.font.Font(font_path, 22)
            self.font_xs  = pygame.font.Font(font_path, 18)
        else:
            self.font_lg  = pygame.font.SysFont(None, 64)
            self.font_md  = pygame.font.SysFont(None, 42)
            self.font_sm  = pygame.font.SysFont(None, 28)
            self.font_xs  = pygame.font.SysFont(None, 22)

        self.state       = STATE_IDLE
        self.order       = None       # 현재 진행 중인 주문
        self.qr_surface  = None
        self.msg         = ""         # 상태 메시지
        self.poll_thread = threading.Thread(target=self.poll_loop, daemon=True)
        self.poll_thread.start()

    # ── 폴링 ───────────────────────────────────────────────
    def poll_loop(self):
        while True:
            try:
                self.poll()
            except Exception as e:
                print("poll error:", e)
            time.sleep(3)

    def poll(self):
        r = requests.get(f"{SERVER}/api/orders/pending", timeout=5)
        orders = r.json()

        if not orders:
            if self.state != STATE_IDLE:
                self.state = STATE_IDLE
                self.order = None
                self.qr_surface = None
            return

        # 가장 최근 PENDING 주문
        order = orders[-1]
        order_id = order["id"]
        pin = order["pinCode"]
        purchase_time = order.get("purchaseTime", "")

        # 주문 시각 기준 60초 후 도착
        try:
            from datetime import datetime, timezone
            pt = datetime.fromisoformat(purchase_time)
            elapsed = (datetime.now() - pt.replace(tzinfo=None)).total_seconds()
            arrived = elapsed >= 60
        except Exception:
            arrived = False

        if arrived:
            if self.state != STATE_ARRIVED or (self.order and self.order["id"] != order_id):
                self.state = STATE_ARRIVED
                self.order = order
                self.qr_surface = self.make_qr(pin)
                # 서버에 DELIVERING 상태 업데이트
                try:
                    requests.post(f"{SERVER}/api/orders/{order_id}/status",
                                  json={"status": "DELIVERING"}, timeout=3)
                except Exception:
                    pass
        else:
            if self.state != STATE_MOVING:
                self.state = STATE_MOVING
                self.order = order

    # ── QR 생성 ────────────────────────────────────────────
    def make_qr(self, pin):
        qr = qrcode.QRCode(box_size=6, border=2)
        qr.add_data(pin)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white")
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        buf.seek(0)
        return pygame.image.load(buf, "qr.png")

    # ── 그리기 유틸 ────────────────────────────────────────
    def text(self, surf, txt, font, color, cx, cy):
        s = font.render(txt, True, color)
        r = s.get_rect(center=(cx, cy))
        surf.blit(s, r)

    def rect_btn(self, surf, x, y, w, h, color, radius=20):
        pygame.draw.rect(surf, color, (x, y, w, h), border_radius=radius)

    # ── IDLE 화면 ──────────────────────────────────────────
    def draw_idle(self):
        s = self.screen
        s.fill(BG)

        # 헤더
        pygame.draw.rect(s, LIGHT, (0, 0, W, 70))
        self.text(s, "PIMTO 자판기", self.font_md, BROWN, W//2, 35)

        # 음료 버튼 2개
        btn_w, btn_h = 280, 200
        gap = 60
        total = btn_w * 2 + gap
        start_x = (W - total) // 2
        btn_y = 130

        self.buttons = []
        for i, p in enumerate(PRODUCTS):
            bx = start_x + i * (btn_w + gap)
            self.rect_btn(s, bx, btn_y, btn_w, btn_h, WHITE)
            pygame.draw.rect(s, LIGHT, (bx, btn_y, btn_w, btn_h), 2, border_radius=20)

            self.text(s, p["name"],            self.font_sm, BROWN,  bx + btn_w//2, btn_y + 60)
            self.text(s, f"{p['price']}원",    self.font_md, ACCENT, bx + btn_w//2, btn_y + 110)
            self.text(s, "눌러서 구매",         self.font_xs, GRAY,   bx + btn_w//2, btn_y + 158)
            self.buttons.append(pygame.Rect(bx, btn_y, btn_w, btn_h))

        # 안내
        self.text(s, "주문 앱/웹에서도 주문 가능합니다", self.font_xs, GRAY, W//2, H - 30)

    # ── MOVING 화면 ────────────────────────────────────────
    def draw_moving(self):
        s = self.screen
        s.fill(BG)

        # 헤더
        pygame.draw.rect(s, BROWN, (0, 0, W, 70))
        self.text(s, "배달 중...", self.font_md, WHITE, W//2, 35)

        # 지도 플레이스홀더
        map_rect = (60, 90, W - 120, H - 160)
        pygame.draw.rect(s, LIGHT, map_rect, border_radius=16)
        pygame.draw.rect(s, GRAY, map_rect, 2, border_radius=16)
        self.text(s, "로봇 이동 중", self.font_md, BROWN, W//2, H//2 - 20)
        self.text(s, "(지도 연동 준비 중)", self.font_xs, GRAY, W//2, H//2 + 25)

        # 상품 이름
        if self.order:
            name = self.order.get("productName", "")
            self.text(s, name, self.font_sm, ACCENT, W//2, H - 35)

    # ── ARRIVED 화면 ───────────────────────────────────────
    def draw_arrived(self):
        s = self.screen
        s.fill(BG)

        # 헤더
        pygame.draw.rect(s, GREEN, (0, 0, W, 70))
        self.text(s, "로봇이 도착했습니다!", self.font_md, WHITE, W//2, 35)

        if self.order:
            pin = self.order.get("pinCode", "----")

            # QR 코드
            if self.qr_surface:
                qr_size = 200
                qr_scaled = pygame.transform.scale(self.qr_surface, (qr_size, qr_size))
                s.blit(qr_scaled, (80, 110))

            # PIN 표시
            pygame.draw.rect(s, WHITE, (340, 110, 380, 200), border_radius=20)
            pygame.draw.rect(s, LIGHT, (340, 110, 380, 200), 2, border_radius=20)
            self.text(s, "PIN 번호",  self.font_sm, GRAY,   530, 155)
            self.text(s, pin,         self.font_lg, BROWN,  530, 210)
            self.text(s, "웹/앱에서 입력해주세요", self.font_xs, GRAY, 530, 260)

            # 상품명
            name = self.order.get("productName", "")
            self.text(s, name, self.font_sm, ACCENT, W//2, H - 35)

    # ── 직접 구매 (IDLE 버튼 클릭) ─────────────────────────
    def direct_purchase(self, product_name):
        self.msg = f"{product_name} 구매 처리 중..."
        # 여기서 아두이노로 배출 신호 보내면 됨 (추후 연동)
        print(f"[직접구매] {product_name}")
        threading.Thread(target=self._show_msg, args=(f"{product_name} 배출 완료!",), daemon=True).start()

    def _show_msg(self, msg):
        self.msg = msg
        time.sleep(2)
        self.msg = ""

    # ── 메인 루프 ─────────────────────────────────────────
    def run(self):
        while True:
            for event in pygame.event.get():
                if event.type == pygame.QUIT:
                    pygame.quit()
                    sys.exit()
                if event.type == pygame.KEYDOWN and event.key == pygame.K_ESCAPE:
                    pygame.quit()
                    sys.exit()
                if event.type == pygame.MOUSEBUTTONDOWN and self.state == STATE_IDLE:
                    for i, btn in enumerate(getattr(self, 'buttons', [])):
                        if btn.collidepoint(event.pos):
                            self.direct_purchase(PRODUCTS[i]["name"])

            if self.state == STATE_IDLE:
                self.draw_idle()
            elif self.state == STATE_MOVING:
                self.draw_moving()
            elif self.state == STATE_ARRIVED:
                self.draw_arrived()

            # 메시지 오버레이
            if self.msg:
                overlay = pygame.Surface((W, 60), pygame.SRCALPHA)
                overlay.fill((61, 44, 30, 220))
                self.screen.blit(overlay, (0, H - 60))
                self.text(self.screen, self.msg, self.font_sm, WHITE, W//2, H - 30)

            pygame.display.flip()
            self.clock.tick(FPS)

if __name__ == "__main__":
    Display().run()

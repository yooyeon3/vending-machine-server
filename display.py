import sys
import io
import threading
import time
import requests
import qrcode
from datetime import datetime

from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QStackedWidget,
    QLabel, QPushButton, QVBoxLayout, QHBoxLayout, QFrame, QSizePolicy
)
from PyQt5.QtCore import Qt, QTimer, pyqtSignal, QObject, QSize
from PyQt5.QtGui import QFont, QPixmap, QImage, QPainter, QColor

SERVER  = "http://192.168.0.67:8080"
IMG_DIR = "/home/pi"

PRODUCTS = [
    {"name": "펩시 콜라",         "price": 1500, "img": f"{IMG_DIR}/pepsi.jpg",   "cmd_top": "drink1_top",    "cmd_bot": "drink1_bottom"},
    {"name": "레쓰비 마일드 커피", "price": 1200, "img": f"{IMG_DIR}/letsbe.jpg",  "cmd_top": "drink2_top",    "cmd_bot": "drink2_bottom"},
]

# ── 아두이노 시리얼 ────────────────────────────────────────
try:
    import serial
    arduino = serial.Serial('/dev/ttyUSB0', 9600, timeout=1)
    time.sleep(2)
    print("아두이노 연결됨")
except Exception as e:
    arduino = None
    print(f"아두이노 연결 실패: {e}")

KIOSK_TIMEOUT = 30  # 키오스크 점유 최대 시간 (초)
_slot_counter = {}  # 제품별 1개 배출 횟수 (top/bottom 교대용)

def _notify_kiosk(state: str):
    try:
        requests.post(f"{SERVER}/api/kiosk/{state}", timeout=3)
    except Exception as e:
        print(f"kiosk notify error: {e}")

def dispense(product, qty):
    if arduino is None:
        print(f"[시뮬] {product['name']} {qty}개 배출")
        return
    try:
        if qty == 2:
            arduino.write((product["cmd_top"] + "\n").encode())
            time.sleep(1)
            arduino.write((product["cmd_bot"] + "\n").encode())
        else:
            n = _slot_counter.get(product["name"], 0)
            cmd = product["cmd_top"] if n % 2 == 0 else product["cmd_bot"]
            arduino.write((cmd + "\n").encode())
            _slot_counter[product["name"]] = n + 1
        print(f"[배출] {product['name']} {qty}개")
    except Exception as e:
        print(f"배출 오류: {e}")

# ── 폴링 ──────────────────────────────────────────────────
class Poller(QObject):
    state_changed = pyqtSignal(str, object)

    def start(self):
        threading.Thread(target=self._loop, daemon=True).start()

    def _loop(self):
        while True:
            try:
                self._check_dispensing()
                self._poll()
            except Exception as e:
                print("poll error:", e)
            time.sleep(3)

    def _check_dispensing(self):
        r = requests.get(f"{SERVER}/api/robot/dispensing", timeout=5)
        orders = r.json()
        for order in orders:
            self._dispense_order(order)
            try:
                requests.post(f"{SERVER}/api/robot/dispensing/{order['id']}/done", timeout=3)
            except Exception as e:
                print("dispense done error:", e)

    def _dispense_order(self, order):
        name = order.get("productName", "")
        # "펩시 콜라 x2, 레쓰비 마일드 커피" 형식 파싱
        for part in name.split(", "):
            qty = 1
            if " x" in part:
                p, q = part.rsplit(" x", 1)
                try:
                    qty = int(q)
                    part = p
                except ValueError:
                    pass
            for product in PRODUCTS:
                if product["name"] in part:
                    dispense(product, qty)
                    break

    def _poll(self):
        r = requests.get(f"{SERVER}/api/robot/delivery/status", timeout=5)
        data = r.json()

        if not data.get("active"):
            self.state_changed.emit("IDLE", None)
            return

        state = data.get("state", "MOVING")
        self.state_changed.emit(state, data)


# ── 공통 스타일 ────────────────────────────────────────────
def header_widget(text, bg="#3d2c1e"):
    w = QWidget()
    w.setFixedHeight(64)
    w.setStyleSheet(f"background:{bg};")
    l = QHBoxLayout(w)
    lbl = QLabel(text)
    lbl.setAlignment(Qt.AlignCenter)
    lbl.setStyleSheet(f"color:white; font-size:22px; font-weight:bold; background:transparent;")
    l.addWidget(lbl)
    return w

def rounded_pixmap(path, size):
    pix = QPixmap(path)
    if pix.isNull():
        pix = QPixmap(size, size)
        pix.fill(QColor("#e8dfd4"))
    pix = pix.scaled(size, size, Qt.KeepAspectRatioByExpanding, Qt.SmoothTransformation)
    # 중앙 크롭
    if pix.width() > size or pix.height() > size:
        x = (pix.width()  - size) // 2
        y = (pix.height() - size) // 2
        pix = pix.copy(x, y, size, size)
    # 둥근 마스크
    result = QPixmap(size, size)
    result.fill(Qt.transparent)
    painter = QPainter(result)
    painter.setRenderHint(QPainter.Antialiasing)
    painter.setBrush(painter.brush())
    from PyQt5.QtGui import QPainterPath
    path = QPainterPath()
    path.addRoundedRect(0, 0, size, size, 16, 16)
    painter.setClipPath(path)
    painter.drawPixmap(0, 0, pix)
    painter.end()
    return result


# ── 왼쪽 메뉴 카드 (이미지 + 이름 + 가격 + -/+ 수량) ──────
class MenuCard(QWidget):
    qty_changed = pyqtSignal(int, int)   # (idx, qty)

    def __init__(self, idx, product):
        super().__init__()
        self.idx = idx
        self.qty = 0
        self.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        self.setStyleSheet("background:white; border-radius:16px;")

        layout = QVBoxLayout(self)
        layout.setContentsMargins(16, 20, 16, 16)
        layout.setSpacing(8)
        layout.setAlignment(Qt.AlignCenter)

        img_lbl = QLabel()
        img_lbl.setAlignment(Qt.AlignCenter)
        img_lbl.setStyleSheet("background:transparent; border:none;")
        pix = rounded_pixmap(product["img"], 160)
        img_lbl.setPixmap(pix)
        layout.addWidget(img_lbl)

        name_lbl = QLabel(product["name"])
        name_lbl.setAlignment(Qt.AlignCenter)
        name_lbl.setWordWrap(True)
        name_lbl.setStyleSheet("font-size:15px; font-weight:bold; color:#3d2c1e; background:transparent; border:none;")
        layout.addWidget(name_lbl)

        price_lbl = QLabel(f"{product['price']:,}원")
        price_lbl.setAlignment(Qt.AlignCenter)
        price_lbl.setStyleSheet("font-size:14px; color:#c47d4a; font-weight:bold; background:transparent; border:none;")
        layout.addWidget(price_lbl)

        qty_row = QHBoxLayout()
        qty_row.setSpacing(8)

        btn_minus = QPushButton("－")
        btn_minus.setFixedSize(40, 40)
        btn_minus.setStyleSheet(self._btn_style())
        btn_minus.clicked.connect(self.decrease)

        self.qty_lbl = QLabel("0")
        self.qty_lbl.setFixedWidth(32)
        self.qty_lbl.setAlignment(Qt.AlignCenter)
        self.qty_lbl.setStyleSheet("font-size:18px; font-weight:bold; color:#3d2c1e; background:transparent; border:none;")

        btn_plus = QPushButton("＋")
        btn_plus.setFixedSize(40, 40)
        btn_plus.setStyleSheet(self._btn_style())
        btn_plus.clicked.connect(self.increase)

        qty_row.addStretch()
        qty_row.addWidget(btn_minus)
        qty_row.addWidget(self.qty_lbl)
        qty_row.addWidget(btn_plus)
        qty_row.addStretch()
        layout.addLayout(qty_row)

    def _btn_style(self):
        return """
            QPushButton {
                background:#f2ede4; color:#3d2c1e; border-radius:10px;
                font-size:18px; font-weight:bold; border:none;
            }
            QPushButton:hover   { background:#e8dfd4; }
            QPushButton:pressed { background:#d4c9ba; }
        """

    def set_qty(self, qty):
        self.qty = qty
        self.qty_lbl.setText(str(qty))

    def decrease(self):
        if self.qty > 0:
            self.qty -= 1
            self.qty_lbl.setText(str(self.qty))
            self.qty_changed.emit(self.idx, self.qty)

    def increase(self):
        if self.qty < 2:
            self.qty += 1
            self.qty_lbl.setText(str(self.qty))
            self.qty_changed.emit(self.idx, self.qty)


# ── 오른쪽 주문 행 (qty > 0 일 때만 표시, 양쪽 동기화) ────
class OrderRow(QWidget):
    qty_changed = pyqtSignal(int, int)   # (idx, qty)

    def __init__(self, idx, product):
        super().__init__()
        self.idx = idx
        self.qty = 0
        self.setFixedHeight(52)
        self.setStyleSheet("background:transparent;")
        self.hide()

        layout = QHBoxLayout(self)
        layout.setContentsMargins(0, 4, 0, 4)
        layout.setSpacing(8)

        name_lbl = QLabel(product["name"])
        name_lbl.setStyleSheet("font-size:14px; color:#3d2c1e; background:transparent; border:none;")
        layout.addWidget(name_lbl, stretch=1)

        btn_minus = QPushButton("－")
        btn_minus.setFixedSize(26, 26)
        btn_minus.setStyleSheet(self._btn_style())
        btn_minus.clicked.connect(self.decrease)

        self.qty_lbl = QLabel("0")
        self.qty_lbl.setFixedWidth(20)
        self.qty_lbl.setAlignment(Qt.AlignCenter)
        self.qty_lbl.setStyleSheet("font-size:13px; font-weight:bold; color:#3d2c1e; background:transparent; border:none;")

        btn_plus = QPushButton("＋")
        btn_plus.setFixedSize(26, 26)
        btn_plus.setStyleSheet(self._btn_style())
        btn_plus.clicked.connect(self.increase)

        layout.addWidget(btn_minus)
        layout.addWidget(self.qty_lbl)
        layout.addWidget(btn_plus)

    def _btn_style(self):
        return """
            QPushButton {
                background:#f2ede4; color:#3d2c1e; border-radius:6px;
                font-size:12px; font-weight:bold; border:none;
            }
            QPushButton:hover   { background:#e8dfd4; }
            QPushButton:pressed { background:#d4c9ba; }
        """

    def set_qty(self, qty):
        self.qty = qty
        self.qty_lbl.setText(str(qty))
        self.setVisible(qty > 0)

    def decrease(self):
        if self.qty > 0:
            self.qty -= 1
            self.qty_lbl.setText(str(self.qty))
            self.setVisible(self.qty > 0)
            self.qty_changed.emit(self.idx, self.qty)

    def increase(self):
        if self.qty < 2:
            self.qty += 1
            self.qty_lbl.setText(str(self.qty))
            self.qty_changed.emit(self.idx, self.qty)

    def reset(self):
        self.qty = 0
        self.qty_lbl.setText("0")
        self.hide()


# ── IDLE 화면 ──────────────────────────────────────────────
class IdlePage(QWidget):
    purchase_done = pyqtSignal(str)

    def __init__(self):
        super().__init__()
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)
        self.setStyleSheet("background:#faf8f4;")

        # 헤더
        header = QWidget()
        header.setFixedHeight(64)
        header.setStyleSheet("background:#3d2c1e;")
        hl = QHBoxLayout(header)
        hl.setContentsMargins(16, 0, 16, 0)

        logo = QLabel()
        logo.setStyleSheet("background:transparent;")
        logo_pix = QPixmap(f"{IMG_DIR}/pimto.png")
        if not logo_pix.isNull():
            logo.setPixmap(logo_pix.scaled(40, 40, Qt.KeepAspectRatio, Qt.SmoothTransformation))

        title = QLabel("PIMTO 자판기")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("color:white; font-size:22px; font-weight:bold; background:transparent;")

        hl.addWidget(logo)
        hl.addWidget(title, stretch=1)
        hl.addSpacing(40)
        layout.addWidget(header)

        # 본문: 왼쪽(메뉴) + 오른쪽(주문 패널)
        body = QWidget()
        body.setStyleSheet("background:transparent;")
        bl = QHBoxLayout(body)
        bl.setContentsMargins(0, 0, 0, 0)
        bl.setSpacing(0)

        # ── 왼쪽: 메뉴 카드 2개 가로 배치
        left = QWidget()
        left.setStyleSheet("background:#faf8f4;")
        ll = QVBoxLayout(left)
        ll.setContentsMargins(16, 16, 16, 12)
        ll.setSpacing(8)

        self.menu_cards = []
        self.order_rows = []

        cards_row = QHBoxLayout()
        cards_row.setSpacing(4)
        for i, p in enumerate(PRODUCTS):
            card = MenuCard(i, p)
            card.qty_changed.connect(self.on_menu_qty_changed)
            cards_row.addWidget(card)
            self.menu_cards.append(card)

        ll.addLayout(cards_row, stretch=1)

        guide = QLabel("수량을 선택하세요")
        guide.setAlignment(Qt.AlignCenter)
        guide.setStyleSheet("font-size:13px; color:#9e8c7a; background:transparent; border:none;")
        ll.addWidget(guide)

        # ── 오른쪽: 주문 패널 (수량 0이면 숨김)
        right = QWidget()
        right.setObjectName("rightPanel")
        self.right_panel = right
        right.hide()
        right.setStyleSheet("#rightPanel { background:white; border-left:2px solid #e8dfd4; }")
        rl = QVBoxLayout(right)
        rl.setContentsMargins(24, 28, 24, 24)
        rl.setSpacing(0)

        order_title = QLabel("주문 내역")
        order_title.setStyleSheet("font-size:18px; font-weight:bold; color:#3d2c1e; background:transparent; border:none;")
        rl.addWidget(order_title)
        rl.addSpacing(12)

        sep_top = QFrame()
        sep_top.setFrameShape(QFrame.HLine)
        sep_top.setFrameShadow(QFrame.Plain)
        sep_top.setStyleSheet("background:#e8dfd4; border:none; max-height:1px;")
        rl.addWidget(sep_top)
        rl.addSpacing(4)

        for i, p in enumerate(PRODUCTS):
            row = OrderRow(i, p)
            row.qty_changed.connect(self.on_order_qty_changed)
            self.order_rows.append(row)
            rl.addWidget(row)

        rl.addStretch()

        sep_bot = QFrame()
        sep_bot.setFrameShape(QFrame.HLine)
        sep_bot.setFrameShadow(QFrame.Plain)
        sep_bot.setStyleSheet("background:#e8dfd4; border:none; max-height:1px;")
        rl.addWidget(sep_bot)
        rl.addSpacing(14)

        self.total_lbl = QLabel("총 0원")
        self.total_lbl.setAlignment(Qt.AlignRight)
        self.total_lbl.setStyleSheet("font-size:20px; font-weight:bold; color:#3d2c1e; background:transparent; border:none;")
        rl.addWidget(self.total_lbl)
        rl.addSpacing(12)

        self.buy_btn = QPushButton("구매하기")
        self.buy_btn.setFixedHeight(56)
        self.buy_btn.setEnabled(False)
        self.buy_btn.setStyleSheet("""
            QPushButton {
                background:#3d2c1e; color:white; border-radius:14px;
                font-size:18px; font-weight:bold; border:none;
            }
            QPushButton:hover   { background:#c47d4a; }
            QPushButton:pressed { background:#2a1e14; }
            QPushButton:disabled { background:#c8c0b8; color:#9a9090; }
        """)
        self.buy_btn.clicked.connect(self.on_buy_all)
        rl.addWidget(self.buy_btn)

        bl.addWidget(left, stretch=4)
        bl.addWidget(right, stretch=1)
        layout.addWidget(body, stretch=1)

        # 카운트다운 라벨 (우측 패널 상단)
        self.countdown_lbl = QLabel("")
        self.countdown_lbl.setAlignment(Qt.AlignRight)
        self.countdown_lbl.setStyleSheet("font-size:13px; color:#c47d4a; background:transparent; border:none;")
        self.countdown_lbl.hide()
        rl.insertWidget(1, self.countdown_lbl)  # 주문 내역 타이틀 바로 아래

        # 30초 키오스크 점유 타이머
        self._remaining = 0
        self._kiosk_busy = False
        self._countdown_timer = QTimer()
        self._countdown_timer.setInterval(1000)
        self._countdown_timer.timeout.connect(self._tick_countdown)

        # 토스트
        self.toast = QLabel("", self)
        self.toast.setAlignment(Qt.AlignCenter)
        self.toast.setStyleSheet("background:#3d2c1e; color:white; border-radius:12px; padding:12px 24px; font-size:15px; font-weight:bold;")
        self.toast.hide()
        self._timer = QTimer()
        self._timer.setSingleShot(True)
        self._timer.timeout.connect(self.toast.hide)

    def on_menu_qty_changed(self, idx, qty):
        self.order_rows[idx].set_qty(qty)
        self.update_total()

    def on_order_qty_changed(self, idx, qty):
        self.menu_cards[idx].set_qty(qty)
        self.update_total()

    def update_total(self):
        total = sum(PRODUCTS[r.idx]["price"] * r.qty for r in self.order_rows)
        self.total_lbl.setText(f"총 {total:,}원")
        self.buy_btn.setEnabled(total > 0)
        self.right_panel.setVisible(total > 0)

        if total > 0 and not self._kiosk_busy:
            self._kiosk_busy = True
            threading.Thread(target=_notify_kiosk, args=("busy",), daemon=True).start()
            self._remaining = KIOSK_TIMEOUT
            self._countdown_timer.start()
            self.countdown_lbl.show()
        elif total == 0 and self._kiosk_busy:
            self._release_kiosk()

    def _tick_countdown(self):
        self._remaining -= 1
        self.countdown_lbl.setText(f"⏱ {self._remaining}초 안에 주문해주세요")
        if self._remaining <= 0:
            for card in self.menu_cards:
                card.set_qty(0)
            for row in self.order_rows:
                row.reset()
            self._release_kiosk()
            self.update_total()

    def _release_kiosk(self):
        self._kiosk_busy = False
        self._countdown_timer.stop()
        self.countdown_lbl.hide()
        threading.Thread(target=_notify_kiosk, args=("free",), daemon=True).start()

    def on_buy_all(self):
        items = [(r.idx, r.qty) for r in self.order_rows if r.qty > 0]
        if not items:
            return
        names = []
        for idx, qty in items:
            product = PRODUCTS[idx]
            threading.Thread(target=dispense, args=(product, qty), daemon=True).start()
            names.append(f"{product['name']} {qty}개")
        summary = ", ".join(names)
        self.show_toast(f"{summary} 배출 중...")
        self.purchase_done.emit(summary)
        for card in self.menu_cards:
            card.set_qty(0)
        for row in self.order_rows:
            row.reset()
        self._release_kiosk()
        self.update_total()

    def show_toast(self, msg):
        self.toast.setText(msg)
        self.toast.adjustSize()
        self.toast.move((self.width() - self.toast.width()) // 2, self.height() - 70)
        self.toast.show()
        self._timer.start(2500)

    def resizeEvent(self, e):
        if self.toast.isVisible():
            self.toast.move((self.width() - self.toast.width()) // 2, self.height() - 70)


# ── MOVING 화면 ────────────────────────────────────────────
class MovingPage(QWidget):
    def __init__(self):
        super().__init__()
        self.setStyleSheet("background:#faf8f4;")
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        layout.addWidget(header_widget("배달 중...", "#c47d4a"))

        body = QWidget()
        bl = QVBoxLayout(body)
        bl.setContentsMargins(40, 30, 40, 20)
        bl.setSpacing(16)

        map_ph = QWidget()
        map_ph.setStyleSheet("background:#f2ede4; border-radius:16px; border:2px solid #e8dfd4;")
        ml = QVBoxLayout(map_ph)
        map_lbl = QLabel("로봇 이동 중\n\n(지도 연동 준비 중)")
        map_lbl.setAlignment(Qt.AlignCenter)
        map_lbl.setStyleSheet("color:#9e8c7a; font-size:18px; background:transparent; border:none;")
        ml.addWidget(map_lbl)
        bl.addWidget(map_ph, stretch=1)

        self.product_lbl = QLabel("")
        self.product_lbl.setAlignment(Qt.AlignCenter)
        self.product_lbl.setStyleSheet("font-size:16px; color:#c47d4a; font-weight:bold; background:transparent;")
        bl.addWidget(self.product_lbl)

        layout.addWidget(body, stretch=1)

    def set_order(self, data):
        if data:
            eta = data.get("eta", "")
            name = data.get("productName", "")
            self.product_lbl.setText(f"{name}  ·  {eta}" if eta else name)


# ── ARRIVED 화면 ───────────────────────────────────────────
class ArrivedPage(QWidget):
    def __init__(self):
        super().__init__()
        self.setStyleSheet("background:#faf8f4;")
        layout = QVBoxLayout(self)
        layout.setContentsMargins(0, 0, 0, 0)
        layout.setSpacing(0)

        layout.addWidget(header_widget("로봇이 도착했습니다!", "#22c55e"))

        body = QWidget()
        bl = QHBoxLayout(body)
        bl.setContentsMargins(60, 40, 60, 30)
        bl.setSpacing(60)
        bl.setAlignment(Qt.AlignCenter)

        # QR
        self.qr_lbl = QLabel()
        self.qr_lbl.setFixedSize(200, 200)
        self.qr_lbl.setAlignment(Qt.AlignCenter)
        self.qr_lbl.setStyleSheet("background:white; border:2px solid #e8dfd4; border-radius:14px;")
        bl.addWidget(self.qr_lbl, alignment=Qt.AlignVCenter)

        # PIN
        pin_box = QWidget()
        pin_box.setStyleSheet("background:white; border-radius:20px; border:2px solid #e8dfd4;")
        pl = QVBoxLayout(pin_box)
        pl.setContentsMargins(30, 30, 30, 30)
        pl.setSpacing(10)
        pl.setAlignment(Qt.AlignCenter)

        pin_title = QLabel("PIN 번호")
        pin_title.setAlignment(Qt.AlignCenter)
        pin_title.setStyleSheet("font-size:14px; color:#9e8c7a; background:transparent; border:none;")

        self.pin_val = QLabel("----")
        self.pin_val.setAlignment(Qt.AlignCenter)
        self.pin_val.setStyleSheet("font-size:40px; font-weight:bold; color:#3d2c1e; letter-spacing:4px; background:transparent; border:none;")

        pin_hint = QLabel("웹/앱에서 입력해주세요")
        pin_hint.setAlignment(Qt.AlignCenter)
        pin_hint.setStyleSheet("font-size:13px; color:#9e8c7a; background:transparent; border:none;")

        self.prod_lbl = QLabel("")
        self.prod_lbl.setAlignment(Qt.AlignCenter)
        self.prod_lbl.setStyleSheet("font-size:16px; color:#c47d4a; font-weight:bold; background:transparent; border:none;")

        pl.addWidget(pin_title)
        pl.addWidget(self.pin_val)
        pl.addWidget(pin_hint)
        pl.addSpacing(12)
        pl.addWidget(self.prod_lbl)

        bl.addWidget(pin_box, alignment=Qt.AlignVCenter)
        layout.addWidget(body, stretch=1)

    def set_order(self, data):
        if not data:
            return
        pin = data.get("pinCode", "----")
        self.pin_val.setText(pin)
        self.prod_lbl.setText(data.get("productName", ""))
        self._make_qr(pin)

    def _make_qr(self, pin):
        try:
            qr = qrcode.QRCode(box_size=6, border=2)
            qr.add_data(pin)
            qr.make(fit=True)
            img = qr.make_image(fill_color="black", back_color="white")
            buf = io.BytesIO()
            img.save(buf, format="PNG")
            buf.seek(0)
            qimg = QImage.fromData(buf.getvalue())
            pix = QPixmap.fromImage(qimg).scaled(196, 196, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            self.qr_lbl.setPixmap(pix)
        except Exception as e:
            print("QR error:", e)


# ── 메인 윈도우 ────────────────────────────────────────────
class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("PIMTO")
        self.showFullScreen()

        self.stack = QStackedWidget()
        self.setCentralWidget(self.stack)

        self.idle_page    = IdlePage()
        self.moving_page  = MovingPage()
        self.arrived_page = ArrivedPage()

        self.stack.addWidget(self.idle_page)
        self.stack.addWidget(self.moving_page)
        self.stack.addWidget(self.arrived_page)

        self._state = "IDLE"

        self.poller = Poller()
        self.poller.state_changed.connect(self.on_state)
        self.poller.start()

    def on_state(self, state, order):
        # 키오스크에서 주문 중이면 IDLE 이외 화면 전환 차단
        if state != "IDLE" and any(c.qty > 0 for c in self.idle_page.menu_cards):
            return
        if state == self._state and state == "MOVING":
            self.moving_page.set_order(order)
            return
        self._state = state
        if state == "IDLE":
            self.stack.setCurrentIndex(0)
        elif state == "MOVING":
            self.moving_page.set_order(order)
            self.stack.setCurrentIndex(1)
        elif state == "ARRIVED":
            self.arrived_page.set_order(order)
            self.stack.setCurrentIndex(2)

    def keyPressEvent(self, e):
        if e.key() == Qt.Key_Escape:
            self.close()


if __name__ == "__main__":
    app = QApplication(sys.argv)
    app.setFont(QFont("Nanum Gothic", 12))
    win = MainWindow()
    sys.exit(app.exec_())

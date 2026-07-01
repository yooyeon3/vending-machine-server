import subprocess

# 1. Get original admin.html from git
result = subprocess.run(['git', 'show', 'HEAD:src/main/resources/templates/admin.html'], stdout=subprocess.PIPE)
original_html = result.stdout.decode('utf-8', errors='ignore')
original_lines = original_html.split('\n')

# 2. Extract CSS (lines 509 to 829, which is index 508 to 828)
css_content = '\n'.join(original_lines[508:829])

# 3. Create fresh admin-chat-panel.html
fresh_html_start = '''    <!-- 관리자 채팅 플로팅 패널 -->
    <div th:fragment="adminChatPanel">
        <style>
'''

fresh_html_end = '''
        </style>
        <div id="chatPanel" class="chat-panel">
            <!-- 화면1: 유저 목록 -->
            <div id="cpScreenList" class="cp-screen active">
                <div class="cp-header">
                    <span class="cp-header-title">💬 고객 채팅</span>
                    <button class="cp-close" onclick="toggleChatPanel()">✕</button>
                </div>
                <div class="cp-chat-list" id="cpChatList">
                    <div class="cp-loading">불러오는 중...</div>
                </div>
            </div>
            <!-- 화면2: 특정 유저와 채팅 -->
            <div id="cpScreenChat" class="cp-screen">
                <div class="cp-header">
                    <button class="cp-back" onclick="showListScreen()">←</button>
                    <span class="cp-header-title" id="cpChatTargetName"></span>
                    <button class="cp-close" onclick="toggleChatPanel()">✕</button>
                </div>
                <div class="cp-messages" id="cpMessages"></div>
                <div class="cp-footer">
                    <input class="cp-input" id="cpInput" placeholder="메시지를 입력하세요..." autocomplete="off">
                    <button class="cp-send" onclick="cpSendAdminMessage()">
                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.5">
                            <line x1="22" y1="2" x2="11" y2="13"></line>
                            <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                        </svg>
                    </button>
                </div>
            </div>
        </div>
        <div id="chatPanelOverlay" class="cp-overlay" onclick="toggleChatPanel()"></div>

        <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
        <script src="https://cdn.jsdelivr.net/npm/stompjs@2.3.3/lib/stomp.min.js"></script>
        <script>
            let cpStompClient = null;
            let cpPanelOpen = false;
            let cpListLoaded = false;
            let cpCurrentUser = null;

            function toggleChatPanel() {
                cpPanelOpen = !cpPanelOpen;
                document.getElementById('chatPanel').classList.toggle('open', cpPanelOpen);
                document.getElementById('chatPanelOverlay').classList.toggle('show', cpPanelOpen);
                if (cpPanelOpen) {
                    showListScreen();
                    if (!cpListLoaded) loadChatList();
                }
            }

            function showListScreen() {
                document.getElementById('cpScreenList').classList.add('active');
                document.getElementById('cpScreenChat').classList.remove('active');
                cpCurrentUser = null;
            }

            function showChatScreen(username) {
                cpCurrentUser = username;
                document.getElementById('cpScreenList').classList.remove('active');
                document.getElementById('cpScreenChat').classList.add('active');
                document.getElementById('cpChatTargetName').textContent = username + '님';
                document.getElementById('cpMessages').innerHTML = '<div class="cp-loading">불러오는 중...</div>';
                loadChatHistory(username);

                // 목록 뱃지 제거
                const item = document.getElementById('cp-list-' + username);
                if (item) {
                    item.classList.remove('has-unread');
                    const badge = item.querySelector('.cp-unread-badge');
                    if (badge) badge.remove();
                }

                // 전체 뱃지 업데이트
                recalcAdminBadge();
            }

            function loadChatList() {
                fetch('/api/chat/list')
                    .then(r => r.json())
                    .then(list => {
                        cpListLoaded = true;
                        renderChatList(list);
                    });
            }

            function renderChatList(list) {
                const container = document.getElementById('cpChatList');
                if (list.length === 0) {
                    container.innerHTML = '<div class="cp-empty-list">아직 채팅을 시작한 고객이 없어요.</div>';
                    return;
                }
                container.innerHTML = '';
                list.forEach(item => {
                    const el = document.createElement('div');
                    el.className = 'cp-list-item' + (item.unreadCount > 0 ? ' has-unread' : '');
                    el.id = 'cp-list-' + item.username;
                    el.onclick = () => showChatScreen(item.username);
                    el.innerHTML = `
                    <div class="cp-list-avatar">${item.username.charAt(0).toUpperCase()}</div>
                    <div class="cp-list-info">
                        <div class="cp-list-name">${escHtml(item.username)}</div>
                        <div class="cp-list-last">${escHtml(item.lastMessage || '')}</div>
                    </div>
                    <div class="cp-list-meta">
                        <span class="cp-list-time">${item.lastTime || ''}</span>
                        ${item.unreadCount > 0 ? '<span class="cp-unread-badge">' + item.unreadCount + '</span>' : ''}
                    </div>`;
                    container.appendChild(el);
                });
            }

            function loadChatHistory(username) {
                fetch('/api/chat/history/' + username)
                    .then(r => r.json())
                    .then(messages => {
                        const container = document.getElementById('cpMessages');
                        container.innerHTML = '';
                        if (messages.length === 0) {
                            container.innerHTML = '<div style="text-align:center;color:var(--sub-text);font-size:13px;margin-top:40px;">아직 대화가 없어요.</div>';
                            return;
                        }
                        messages.forEach(msg => cpRenderMessage(msg, username, false));
                        container.scrollTop = container.scrollHeight;
                    });
            }

            function cpRenderMessage(msg, username, scroll) {
                const container = document.getElementById('cpMessages');
                const loadingEl = container.querySelector('.cp-loading');
                if (loadingEl) loadingEl.remove();

                const row = document.createElement('div');
                const isAdmin = msg.fromAdmin;
                row.className = isAdmin ? 'cp-msg-row mine' : 'cp-msg-row';
                const time = msg.time || new Date().toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit', hour12: false });
                const name = username || msg.userUsername || '';

                if (isAdmin) {
                    row.innerHTML = `<div class="cp-msg-content"><div class="cp-bubble mine">${escHtml(msg.content)}</div><span class="cp-time">${time}</span></div>`;
                } else {
                    row.innerHTML = `<div class="cp-avatar">${name.charAt(0).toUpperCase()}</div><div class="cp-msg-content"><span class="cp-sender">${escHtml(name)}</span><div class="cp-bubble user">${escHtml(msg.content)}</div><span class="cp-time">${time}</span></div>`;
                }
                container.appendChild(row);
                if (scroll !== false) container.scrollTop = container.scrollHeight;
            }

            function cpSendAdminMessage() {
                const input = document.getElementById('cpInput');
                const content = input.value.trim();
                if (!content || !cpStompClient || !cpCurrentUser) return;
                cpStompClient.send('/app/chat/admin/send', {}, JSON.stringify({ content: content, targetUsername: cpCurrentUser }));
                cpRenderMessage({ content: content, fromAdmin: true }, cpCurrentUser);
                input.value = '';
            }

            function updateListItemWithNewMsg(msg) {
                const username = msg.userUsername;
                let item = document.getElementById('cp-list-' + username);

                if (!item) {
                    item = document.createElement('div');
                    item.className = 'cp-list-item has-unread';
                    item.id = 'cp-list-' + username;
                    item.onclick = () => showChatScreen(username);
                    item.innerHTML = `
                    <div class="cp-list-avatar">${username.charAt(0).toUpperCase()}</div>
                    <div class="cp-list-info">
                        <div class="cp-list-name">${escHtml(username)}</div>
                        <div class="cp-list-last"></div>
                    </div>
                    <div class="cp-list-meta">
                        <span class="cp-list-time"></span>
                        <span class="cp-unread-badge">0</span>
                    </div>`;
                    const emptyEl = document.querySelector('.cp-empty-list');
                    if (emptyEl) emptyEl.remove();
                    const list = document.getElementById('cpChatList');
                    list.insertBefore(item, list.firstChild);
                }

                item.querySelector('.cp-list-last').textContent = msg.content;
                item.querySelector('.cp-list-time').textContent = msg.time;

                if (cpCurrentUser !== username) {
                    item.classList.add('has-unread');
                    let badge = item.querySelector('.cp-unread-badge');
                    if (!badge) {
                        badge = document.createElement('span');
                        badge.className = 'cp-unread-badge';
                        badge.textContent = '0';
                        item.querySelector('.cp-list-meta').appendChild(badge);
                    }
                    badge.textContent = (parseInt(badge.textContent) || 0) + 1;

                    const globalBadge = document.getElementById('chatBadge');
                    if (globalBadge) {
                        const cur = parseInt(globalBadge.textContent) || 0;
                        globalBadge.textContent = cur + 1;
                        globalBadge.style.display = 'flex';
                    }
                }

                const list = document.getElementById('cpChatList');
                list.insertBefore(item, list.firstChild);
            }

            function recalcAdminBadge() {
                const totalUnread = document.querySelectorAll('.cp-unread-badge').length;
                const badge = document.getElementById('chatBadge');
                if (!badge) return;
                if (totalUnread > 0) {
                    badge.style.display = 'flex';
                } else {
                    badge.style.display = 'none';
                }
            }

            function escHtml(t) { return String(t).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

            document.getElementById('cpInput').addEventListener('keydown', function (e) {
                if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); cpSendAdminMessage(); }
            });

            (function connectWS() {
                const socket = new SockJS('/ws');
                cpStompClient = Stomp.over(socket);
                cpStompClient.debug = null;
                cpStompClient.connect({}, function () {
                    cpStompClient.subscribe('/user/queue/chat', function (frame) {
                        const msg = JSON.parse(frame.body);
                        if (msg.fromAdmin) return;

                        if (cpCurrentUser === msg.userUsername) {
                            cpRenderMessage(msg, msg.userUsername);
                        } else {
                            if (cpListLoaded) updateListItemWithNewMsg(msg);
                        }
                    });
                });
            })();
        </script>
    </div>
'''

fragment_path = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/fragments/admin-chat-panel.html'
with open(fragment_path, 'w', encoding='utf-8') as f:
    f.write(fresh_html_start + css_content + fresh_html_end)
print('Successfully fixed admin-chat-panel.html!')

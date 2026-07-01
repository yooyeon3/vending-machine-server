import subprocess

result = subprocess.run(['git', 'show', 'HEAD:src/main/resources/templates/admin.html'], stdout=subprocess.PIPE)
original_html = result.stdout.decode('utf-8')
lines = original_html.split('\n')

start_idx = -1
end_idx = -1

for i, line in enumerate(lines):
    if '<div id="chatPanel" class="chat-panel">' in line:
        start_idx = i - 1 
    if '})();' in line and start_idx != -1:
        if i+1 < len(lines) and '</script>' in lines[i+1]:
            end_idx = i + 1
            break

if start_idx != -1 and end_idx != -1:
    fragment_content = '<div th:fragment="adminChatPanel">\n' + '\n'.join(lines[start_idx:end_idx+1]) + '\n</div>\n'
    with open('src/main/resources/templates/fragments/admin-chat-panel.html', 'w', encoding='utf-8') as f:
        f.write(fragment_content)
    print('Successfully generated admin-chat-panel.html')
else:
    print('Could not find boundaries: start=', start_idx, 'end=', end_idx)

file_path = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/admin.html'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
new_lines = lines[:982] + ['    <div th:replace=\"~{fragments/admin-chat-panel :: adminChatPanel}\"></div>\n'] + lines[1228:]
with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(new_lines)

# Also add to admin_statistics.html
file_path_stats = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/admin_statistics.html'
with open(file_path_stats, 'r', encoding='utf-8') as f:
    lines_stats = f.readlines()
# insert before </body>
for i in range(len(lines_stats)-1, -1, -1):
    if '</body>' in lines_stats[i]:
        lines_stats.insert(i, '    <div th:replace=\"~{fragments/admin-chat-panel :: adminChatPanel}\"></div>\n')
        break
with open(file_path_stats, 'w', encoding='utf-8') as f:
    f.writelines(lines_stats)

# Also add to admin-member.html
file_path_member = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/admin-member.html'
with open(file_path_member, 'r', encoding='utf-8') as f:
    lines_member = f.readlines()
for i in range(len(lines_member)-1, -1, -1):
    if '</body>' in lines_member[i]:
        lines_member.insert(i, '    <div th:replace=\"~{fragments/admin-chat-panel :: adminChatPanel}\"></div>\n')
        break
with open(file_path_member, 'w', encoding='utf-8') as f:
    f.writelines(lines_member)

# Also add to robot-control.html
file_path_robot = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/robot-control.html'
with open(file_path_robot, 'r', encoding='utf-8') as f:
    lines_robot = f.readlines()
for i in range(len(lines_robot)-1, -1, -1):
    if '</body>' in lines_robot[i]:
        lines_robot.insert(i, '    <div th:replace=\"~{fragments/admin-chat-panel :: adminChatPanel}\"></div>\n')
        break
with open(file_path_robot, 'w', encoding='utf-8') as f:
    f.writelines(lines_robot)

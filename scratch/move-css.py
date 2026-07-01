file_path_admin = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/admin.html'
with open(file_path_admin, 'r', encoding='utf-8') as f:
    lines = f.readlines()

start_idx = 508  # 0-indexed for line 509
end_idx = 829    # 0-indexed for line 830

css_content = ''.join(lines[start_idx:end_idx])

# Now append this CSS to admin-chat-panel.html
fragment_path = 'c:/Users/yooye/Downloads/PIMTO/vendingmachine (1)/vendingmachine/src/main/resources/templates/fragments/admin-chat-panel.html'
with open(fragment_path, 'r', encoding='utf-8') as f:
    fragment_content = f.read()

insert_pos = fragment_content.find('<div id="chatPanel"')
new_fragment = fragment_content[:insert_pos] + '<style>\n' + css_content + '\n</style>\n' + fragment_content[insert_pos:]

with open(fragment_path, 'w', encoding='utf-8') as f:
    f.write(new_fragment)

print('Appended CSS to fragment.')

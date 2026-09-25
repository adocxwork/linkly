with open('src/main/java/com/gupta/linkly/service/UserService.java', 'r') as f:
    content = f.read()

content = content.replace(
    '        userRepository.delete(user);\n    }\n\n    private User getUserByUsername(String username) {',
    '        if (user.getLinks() != null) {\n            for (com.gupta.linkly.entity.Link link : user.getLinks()) {\n                stringRedisTemplate.delete("redirect:" + link.getShortUrl());\n            }\n        }\n        userRepository.delete(user);\n    }\n\n    private User getUserByUsername(String username) {'
)

with open('src/main/java/com/gupta/linkly/service/UserService.java', 'w') as f:
    f.write(content)

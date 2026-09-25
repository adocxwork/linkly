#!/bin/bash
sed -i '' '/import java.util.UUID;/a\
import org.springframework.data.jpa.repository.Modifying;\
import org.springframework.data.jpa.repository.Query;\
import org.springframework.data.repository.query.Param;' src/main/java/com/gupta/linkly/repository/LinkRepository.java

sed -i '' '/long countByUser(User user);/a\
\    @Modifying\
    @Query("UPDATE Link l SET l.clickCount = l.clickCount + 1 WHERE l.id = :id")\
    void incrementClickCount(@Param("id") UUID id);' src/main/java/com/gupta/linkly/repository/LinkRepository.java

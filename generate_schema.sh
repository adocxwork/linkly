mvn compile
mvn exec:java -Dexec.mainClass="org.hibernate.tool.hbm2ddl.SchemaExport" -Dexec.classpathScope=compile

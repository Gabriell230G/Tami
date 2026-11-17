# Tami: imagem para publicar o app (Render, Railway, Fly...). Compila dentro da imagem, sem Maven.
FROM eclipse-temurin:21-jdk AS compila
WORKDIR /app
COPY lib lib
COPY src src
COPY sql sql
RUN mkdir -p build/classes \
 && javac --release 17 -encoding UTF-8 -cp "lib/*" -d build/classes $(find src/main/java -name '*.java') \
 && cp -r src/main/resources/* build/classes/ \
 && mkdir -p build/classes/sql && cp -r sql/* build/classes/sql/

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compila /app/build/classes build/classes
COPY lib lib
COPY web web
COPY dados/validacao.json dados/validacao.json
COPY dados/simulados/painel.json dados/simulados/painel.json
ENV TAMI_MODO=demonstracao
EXPOSE 8080
# 512 MB no plano grátis: deixa folga para a JVM
CMD ["java", "-Xmx300m", "-XX:+UseSerialGC", "-cp", "build/classes:lib/*", "br.com.fiap.tami.app.App", "servidor"]

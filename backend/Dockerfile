# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# As dependências mudam muito menos que o código: em uma camada própria,
# o Docker reaproveita o cache e o build do dia a dia fica em segundos.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre-alpine

# Nunca rodar como root dentro do contêiner.
RUN addgroup -S app && adduser -S -G app app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -q -O- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

# MaxRAMPercentage faz a JVM respeitar o limite de memória do contêiner,
# em vez de olhar a RAM total da VPS.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

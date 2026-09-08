# Dockerfile
FROM mcr.microsoft.com/mssql/server:2022-latest

USER root
WORKDIR /usr/src/app

# Copiar scripts de inicialización
COPY migration_v1_initial.sql .
COPY entrypoint.sh .

# Dar permisos al script de arranque
RUN chmod +x entrypoint.sh

USER mssql

ENV ACCEPT_EULA=Y
ENV MSSQL_SA_PASSWORD=UserP@ssw0rd!
ENV MSSQL_PID=Express

EXPOSE 1433

CMD ["/bin/bash", "./entrypoint.sh"]

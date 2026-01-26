# Dockerfile
FROM mcr.microsoft.com/mssql/server:2022-latest

USER root
WORKDIR /usr/src/app

# Copiar scripts y EL BACKUP
COPY init.sql .
COPY entrypoint.sh .
COPY CaichDB.bak .

# Dar permisos (Importante para que el usuario mssql pueda leer el backup)
RUN chmod +x entrypoint.sh && \
    chown mssql:root CaichDB.bak

USER mssql

ENV ACCEPT_EULA=Y
ENV SA_PASSWORD=YourStrong!Passw0rd
ENV MSSQL_PID=Express

EXPOSE 1433

CMD ["/bin/bash", "./entrypoint.sh"]
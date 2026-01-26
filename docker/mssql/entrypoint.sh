#!/bin/bash
# entrypoint.sh

# Arrancar SQL Server en segundo plano
/opt/mssql/bin/sqlservr &
pid=$!

echo "Esperando a que SQL Server inicie..."
# Esperar hasta 60 segundos
for i in {1..60};
do
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -Q "SELECT 1" > /dev/null 2>&1
    if [ $? -eq 0 ]
    then
        echo "SQL Server está listo."
        break
    else
        echo "Cargando..."
        sleep 1
    fi
done

# Ejecutar script de restauración
echo "Ejecutando restauración e inicialización..."
/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d master -i /usr/src/app/init.sql

echo "Proceso finalizado. Logs del servidor:"
wait $pid
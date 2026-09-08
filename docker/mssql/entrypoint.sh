#!/bin/bash
# entrypoint.sh

# Arrancar SQL Server en segundo plano
/opt/mssql/bin/sqlservr &
pid=$!

echo "Esperando a que SQL Server inicie..."
# Esperar hasta 60 segundos
for i in {1..60};
do
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -Q "SELECT 1" > /dev/null 2>&1
    if [ $? -eq 0 ]
    then
        echo "SQL Server está listo."
        break
    else
        echo "Cargando..."
        sleep 1
    fi
done

# Inicializar solo la primera vez; el volumen conserva los datos en reinicios.
if /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -h -1 -W \
    -Q "SET NOCOUNT ON; IF DB_ID(N'CaichDB') IS NULL SELECT 'MISSING' ELSE SELECT 'EXISTS'" | grep -q '^EXISTS$'; then
    echo "CaichDB ya existe. Se conserva la información actual."
else
    echo "Inicializando CaichDB por primera vez..."
    /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -d master -i /usr/src/app/migration_v1_initial.sql
fi

echo "Proceso finalizado. Logs del servidor:"
wait $pid

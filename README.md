# CAICH API

API de CAICH basada en Spring Boot. Para desarrollo usa la configuración local
del proyecto. Para producción en Windows Server con IIS, la guía única es
`../caich-front/DEPLOYMENT_IIS.md` (los repositorios deben quedar como carpetas
hermanas en `C:\CAICH`).

No copies el JAR ni ejecutes Java manualmente en el servidor. Después de la
instalación inicial, desde esta carpeta ejecuta:

```powershell
.\scripts\windows\deploy-caich.ps1 -Component All
```

El proceso publica los artefactos en IIS y reinicia los servicios `CAICH-API`
y `CAICH-Frontend` de manera ordenada.

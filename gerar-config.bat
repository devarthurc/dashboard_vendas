@echo off


cd /d "%~dp0"

if not exist "dashboard_vendas.jar" (
    echo.
    echo ERRO: nao encontrei o arquivo dashboard_vendas.jar nesta pasta.
    echo Coloque este .bat na mesma pasta do dashboard_vendas.jar e tente novamente.
    echo.
    pause
    exit /b 1
)

java -cp dashboard_vendas.jar GeradorConfig

if errorlevel 1 (
    echo.
    echo Ocorreu um erro ao abrir a tela de configuracao.
    echo Verifique se o Java esta instalado e disponivel no PATH.
    echo.
    pause
)

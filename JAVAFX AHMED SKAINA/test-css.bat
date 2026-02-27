@echo off
echo ================================
echo   TEST CSS JAVAFX - PRODUITS
echo ================================
echo.
echo 1. Verification des fichiers...

if exist "src\main\resources\produits\produit-style.css" (
    echo [OK] produit-style.css existe dans src
) else (
    echo [ERREUR] produit-style.css manquant dans src
)

if exist "target\classes\produits\produit-style.css" (
    echo [OK] produit-style.css existe dans target
) else (
    echo [ERREUR] produit-style.css manquant dans target
)

if exist "src\main\resources\produits\crudProduit.fxml" (
    echo [OK] crudProduit.fxml existe dans src
) else (
    echo [ERREUR] crudProduit.fxml manquant dans src
)

echo.
echo 2. Taille des fichiers...
for %%f in ("src\main\resources\produits\produit-style.css") do echo CSS source: %%~zf octets
for %%f in ("target\classes\produits\produit-style.css") do echo CSS target: %%~zf octets

echo.
echo 3. Verification du debut du CSS...
powershell -Command "Get-Content 'src\main\resources\produits\produit-style.css' | Select-Object -First 3"

echo.
echo 4. Verification des classes CSS importantes...
findstr /C:".btn {" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn definie
findstr /C:".btn-blue" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn-blue definie
findstr /C:".btn-green" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn-green definie
findstr /C:".btn-orange" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn-orange definie
findstr /C:".btn-red" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn-red definie
findstr /C:".btn-gray" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .btn-gray definie
findstr /C:".wide" "src\main\resources\produits\produit-style.css" >nul && echo [OK] .wide definie

echo.
echo ================================
echo   Verification terminee!
echo ================================
echo.
echo Maintenant, relancez votre application JavaFX.
echo Verifiez la console pour les messages de chargement CSS.
echo.
pause


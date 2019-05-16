cd ..\AlgoJL_Rework
for ($i = 1; $i -lt 81; $i++) {
    Start-Process java -ea -cp ".\bin;.\librairies\peersim-1.0.5.jar;.\librairies\jep-2.3.0.jar;.\librairies\djep-1.0.0.jar" peersim.Simulator "..\config_file\config_$i_.txt"
}
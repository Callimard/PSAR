﻿cd ..\Algo_JL_Loan
for ($i = 1; $i -lt 81; $i++) {
    java -ea -cp ".\bin;.\librairies\peersim-1.0.5.jar;.\librairies\jep-2.3.0.jar;.\librairies\djep-1.0.0.jar" peersim.Simulator ".\src\config_$i.txt"
}
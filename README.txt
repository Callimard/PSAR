README :

Dans le cadre de ce projet, nous avons utilisé l'IDE Eclipse. Les instructions qui suivent ne fonctionnent donc qu'avec cet IDE :

- Commencez par importer l'un des deux projets dans Eclipse. AlgoJL_Rework correspond à la version sans mécanisme de prêt et est fonctionnelle. Algo_JL_Loan correspond à la version avec mécanisme de prêt mais ne fonctionne malheureusement pas. Les instructions qui suivent concernent le projet AlgoJL_Rework.

- Une fois le projet importé, créez une configuration de lancement en faisant un clic droit dessus -> "Run as" -> "Run Configurations..."

- Dans la fenêtre qui s'ouvre, créez une nouvelle configuration "Java Application"

- Dans l'onglet "Main", dans la case "Main class", écrire "peersim.Simulator"

- Dans l'onglet "Arguments", dans la case "Program arguments", écrire "src/config-main.txt"

- Lancez le prgramme avec la configuration nouvellement créée.

A noter : 

Dans ce fichier de configuration, vous pouvez faire varier les valeurs suivantes:

- network.size indiquie le nombre de noeuds du système
- protocol.algoJL.nb_resource indique le nombre de ressources
- protocol.algoJL.nbCS indique le nombre de sections critiques que doit efectuer chaque noeud (-1 = infini)
- protocol.transportPID.mindelay et .maxdelay permet de faire varier la latence du réseau

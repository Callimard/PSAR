# Sortie
set terminal postscript eps color enhanced "Times-Roman" 22
#set terminal png
set output "graphe_comparaison_sans_pret.ps"
set encoding utf8
# Paramètres
#set key inside top right
set key at 52,59
set xrange [1:*]
set yrange [0:60]
set style fill solid border 3
set xtic rotate by -45 scale 0
set xlabel "Maximum size of requests" font "Times-Roman , 32"
set ylabel "Resources use rate" font "Times-Roman , 32"
set nolabel
set notitle
set datafile separator ";"
#set logscale x 2
plot [] "rate_use-0,1.csv" using 1:4 title "Without loan" with linespoint linewidth 3 linetype 1 pt 10 lc 5,"" using 1:5 title "With loan" with linespoint linewidth 3 linetype 1 pt 12 lc 0,"sans_prêt.csv" using 1:3 title "Our curve" with linespoint linewidth 3 linetype 1 pt 2 lc 1

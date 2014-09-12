for i
do
  sox "$i" -t raw -r 8192 -e signed-integer -c 1 - | java -jar dist/lib/Fpff-20140912.jar > "$i.fpff"
done

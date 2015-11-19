# Rename all *.txt to *.text
for f in *.jar; do 
mv -- "$f" "${f%.jar}.zip"
done
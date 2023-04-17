for i in {1..40}; do find . -name pom.xml | while read file; do echo "<--no comment -->" >> $file; done && git add . && git commit -m "chore: a comment" && git push -u origin -f feat/using-workspaces; date ; sleep 5; done


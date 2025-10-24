
# ERP Didático

Um ERP didático utilizado na disciplina de Administração do curso de graduação em Ciência da Computação.


## Usage

#### Compile:
```sh
javac -d bin/ -cp "lib/sqlite-jdbc-3.50.3.0.jar;lib/weka.jar" src/com/erp/*.java -Werror -verbose -encoding UTF-8

jar cfe builds/erp.jar com.erp.Main -C bin/ .
```

#### Run:
```sh
java --enable-native-access=ALL-UNNAMED -cp "builds/erp.jar;lib/sqlite-jdbc-3.50.3.0.jar;lib/weka.jar" com.erp.Main
```

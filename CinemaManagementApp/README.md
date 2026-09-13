ΟΔΗΓΙΕΣ

backend:
1.ΑΝΟΙΓΟΥΜΕ cmd Η ΕΝΑ terminal ΣΤΟ vscode
2.ΓΡΑΦΟΥΜΕ ΤΗΝ ΕΝΤΟΛΗ mvnw spring-boot:run
3.ΑΝΟΙΓΟΥΜΕ ΣΤΟ browser ΤΗ URL: http://localhost:8080/h2
4.ΣΤΗΝ ΟΘΟΝΗ ΤΟΥ H2 Console ΒΑΖΟΥΜΕ JDBC URL: jdbc:h2:mem:devdb, User Name: sa ΚΑΙ Password ΚΕΝΟ(ΑΝ ΔΕΝ ΑΝΟΙΞΕΙ ΔΟΚΙΜΑΖΟΥΜΕ admin και admin123 Η admin και admin1234 - ΑΥΤΟ ΣΥΝΗΘΩΣ ΧΡΕΙΑΖΕΤΑΙ ΜΟΝΟ ΜΙΑ ΦΟΡΑ ΚΑΙ ΜΕΤΑ ΑΝ ΘΕΛΟΥΜΕ ΝΑ ΞΑΝΑΕΙΣΕΛΘΟΥΜΕ ΜΠΑΝΟΥΜΕ ΚΑΝΟΝΙΚΑ ΜΕ ΑΠΛΟ connect ΜΕ ΑΥΤΑ ΠΟΥ ΕΙΝΑΙ ΗΔΗ ΣΤΑ ΠΕΔΙΑ ΔΗΛΑΔΗ sa ΚΑΙ ΚΕΝΟ password)
5.ΠΑΤΑΜΕ connect ΚΑΙ ΑΝΟΙΓΕΙ Η ΒΑΣΗ ΜΕ ΤΑ ΔΟΚΙΜΑΣΤΙΚΑ ΔΕΔΟΜΕΝΑ ΠΟΥ ΕΧΟΥΝ ΓΙΝΕΙ seed ΑΠΟ ΤΟ ΑΡΧΕΙΟ data.sql(ΤΑ ΔΕΔΟΜΕΝΑ ΑΥΤΑ ΕΙΝΑΙ ΜΟΝΟ ΚΑΙ ΜΟΝΟ ΓΙΑ ΝΑ ΔΟΥΜΕ ΑΝ Η ΒΑΣΗ ΚΑΙ ΟΙ ΠΙΝΑΚΕΣ ΛΕΙΤΟΥΡΓΟΥΝ ΣΩΣΤΑ ΚΑΙ ΔΕΝ ΘΑ ΤΑ ΧΡΗΣΙΜΟΠΟΙΗΣΟΥΜΕ ΠΟΥΘΕΝΑ ΑΛΛΟΥ)


frontend:
1.ΑΝΟΙΓΟΥΜΕ cmd Η ΕΝΑ terminal στο vscode
2.ΜΕΤΑ ΓΡΑΦΟΥΜΕ npm install ΑΝ ΔΕΝ ΤΟ ΕΧΟΥΜΕ ΚΑΝΕΙ ΗΔΗ
3.ΜΕΤΑ cd cinema-frontend ΚΑΙ ΜΕΤΑ npm run dev
4.ΚΑΝΟΥΜΕ copy ΤΟ url: http://localhost:5173/ ΠΟΥ ΜΑΣ ΒΓΑΖΕΙ ΚΑΙ ΤΟ ΚΑΝΟΥΜΕ paste στο browser

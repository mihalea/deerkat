# Deerkat
Deerkat is an application written in Java that imports transaction data from statement files downloaded from HSBC UAE in HTML format. It can be used to categorise the transactions based on the merchant and export them into a format compatible with YNAB4.

# Changelog

- **v0.1:** Added HtmlProcessor that handles parsing html files using Jsoup.
- **v0.2:** Added storage in a local Sqlite database.
- **v0.3:** Added storage in a local CSV file. This is not a lossless procedure as the CSV stores less data than extracted.
- **v0.4:** Added a graphical user interface using JavaFx that allows users to import HTML files, view the data and export it to a CSV file

# Credits

Icon created from assets by [Freepik](http://www.freepik.com/) from [www.flaticon.com ](www.flaticon.com )
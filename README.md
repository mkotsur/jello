This tool enables Trello/Jira integration. More details will follow later.

[![Circle CI](https://circleci.com/gh/mkotsur/jello.svg?style=svg)](https://circleci.com/gh/mkotsur/jello)


# Starting

You need to pass the following java properties in order to start the tool:

`-Djira.username=yourjirausername`

`-Djira.password=yourjirapassword`

`-Dtrello.appToken=very-long-74-characters-app-token`

To get an app token you need to visit this URL:

`https://trello.com/1/authorize?key=57c8a483b823e416f5d25a2dab456f12&name=Jello&expiration=30days&response_type=token&scope=read,write`


Please note, that token expires every 30 days.

# Tickets range

Can be specified as a comma separated sequence of tickets and ranges. Range has a format: `{i}@{start-with-ticket}..{end-with-ticket}`, where `i` is rapid board id.

e.g.: `4@DEPL-1234..DEPL-1278, DEPL-5555, DEPL-6666`.
$(document).ready(function() {
    $('td[id|="document"]').click(function() {
        $(this).children("div").slideToggle();
    });
});
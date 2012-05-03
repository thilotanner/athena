$(document).ready(function() {
    $('span[id|="document"]').click(function() {
        $(this).siblings("div").slideToggle();
    });
});
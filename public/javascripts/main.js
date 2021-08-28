$(function() {
    var copyClick = function () {
        $(".copyable").click(function(e) {
            navigator.clipboard.writeText($(e.target).text());
        });
    }

    $('#make-a-room button').click(function(e) {
        e.preventDefault();
        $.ajax({
          type: 'POST',
          url: '/createRoom',
          headers: {
              'Content-Type': 'application/json'
          },
          success: function(data) {
            $('#make-a-room, #join-a-room').hide(400);
            $('#room-info').html(
              'Welcome to <span class="copyable">' + data['id'] + '</span><br>' +
              '<span class="small-note">(Click to copy to clipboard.)</span>'
            );
            $('#room-info').show(400);
            copyClick();
            makeWSConnection(data['id']);
          }
        });
    });

    $('#join-a-room button').click(function(e) {
        e.preventDefault();
        $('#make-a-room, #join-a-room button').hide(400);
        $('#join-a-room input').show(400);
    });

    $('#join-a-room input').on('keypress', function(e) {
         if(e.which == 13) {
             e.preventDefault();
             makeWSConnection($(this).val());
             $('#join-a-room').hide(400);
             $('#room-info').html(
                  'Welcome to <span class="copyable">' + $(this).val() + '</span><br>' +
                   '<span class="small-note">(Click to copy to clipboard.)</span>'
             );
             $('#room-info').show(400);
         }
    });

    const makeWSConnection = function(id) {
        hostUrl = $("#sidebar").attr('host-url');
        ws = new WebSocket("ws://" + hostUrl + ":9000/room/" + id);
        ws.onmessage = function(event) {
            var data = JSON.parse(event.data)
            $('#chat-display').append('<div class=message>' + data.text + '</div>');
        };

        $('#message-area').on('keypress', function(e) {
            if(e.which == 13) {
                e.preventDefault();
                ws.send(JSON.stringify({ type: "message", text: $(this).val()}));
                $(this).val("");
            }
        });
    };

});
$(function() {

    $('#make-a-room button').click(function(e) {
        e.preventDefault();
        $.ajax({
          type: 'POST',
          url: '/createRoom',
          headers: {
              'Content-Type': 'application/json'
          },
          success: function(data) {
            makeWSConnection(data['id']);
          }
        });
    });

    $('#join-a-room button').click(function(e) {
        e.preventDefault();
        makeWSConnection($("#join-a-room input").val());
    });


    const makeWSConnection = function(id) {
        ws = new WebSocket("ws://192.168.0.5:9000/room/" + id);
        ws.onmessage = function(event) {
          console.debug("WebSocket message received:", event);
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
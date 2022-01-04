$(function() {
    var copyClick = function () {
        $(".copyable").click(function(e) {
            navigator.clipboard.writeText($(e.target).text());
        });
    }

    var setIntro = function(name, color) {
        $("#intro").html("Hello, <span style='color: " + color + ";'>" + name + "</span>!");
    };

    var getNameAndColor = function() {
        storage = window.localStorage;
        localName = storage.getItem("alias");
        localColor = storage.getItem("color");

        if(localName ===  null || localName == "") {
             localName = $("body").attr("data-name");
             storage.setItem("alias", localName);
        }

        if(localColor ===  null || localColor == "") {
            localColor = $("body").attr("data-color");
            console.log(localColor);
            storage.setItem("color", localColor);
        }

        return [localName, localColor];
    }

    var [name, color] = getNameAndColor();
    setIntro(name, color);

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
        $('#room-info').html('');
        $('#join-a-room input').show(400);
    });

    $('#join-a-room input').on('keypress', function(e) {
         if(e.which == 13) {
             e.preventDefault();
             makeWSConnection($(this).val());
             $('#join-a-room').hide(400);
             $('#join-a-room button').show(400);
             $('#join-a-room input').hide(400);
             $('#room-info').html(
                  'Welcome to <span class="copyable">' + $(this).val() + '</span><br>' +
                  '<span class="small-note">(Click to copy to clipboard.)</span>'
             );
             $('#room-info').show(400);
         }
    });

    const makeWSConnection = function(id) {
        hostUrl = $("#sidebar").attr('host-url');

        ws = new WebSocket("ws://" + hostUrl + ":9000/room/" + id + "/" + name);
        ws.onmessage = function(event) {
            var data = JSON.parse(event.data)
            $('#chat-display').append('<div class="header" style="color: ' + data.color + ';">' + data.name + '</div><div class="message">' + data.text + '</div>');
        };
        ws.onclose = function(event) {
            $('#room-info').html('Your connection <span style="color: #ff0000;">failed</span>. You either timed out or put the name in wrong.');
            $('#make-a-room, #join-a-room').show(400);
            $('#chat-display').html('');
            $('#message-area').off('keypress');
        }

        $('#message-area').on('keypress', function(e) {
            if(e.which == 13) {
                e.preventDefault();
                ws.send(JSON.stringify({ type: "message", text: $(this).val(), name: localName, color: localColor}));
                $(this).val("");
            }
        });
    };

});
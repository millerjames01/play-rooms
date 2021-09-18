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

    const makeWSConnection = function(name, color) {
        hostUrl = $("#sidebar").attr('host-url');
        console.log("ws://" + hostUrl + ":9000/connect/" + name + "/" + color.substring(1));
        ws = new WebSocket("ws://" + hostUrl + ":9000/connect/" + name + "/" + color.substring(1));
        ws.onmessage = function(event) {
            var data = JSON.parse(event.data)
            switch (data.type) {
                case "message":
                    $('#chat-display').append('<div class="header" style="color: ' + data.color + ';">' + data.name + '</div><div class="message">' + data.text + '</div>');
                    break;
                case "room-joined":
                    $('#make-a-room, #join-a-room button').hide(400);
                    $('#join-a-room input').show(400);
                    $('#room-info').html(
                       data.message
                    );
                    break;
                case "timeout-error":
                    $('#chat-display').append('<div class="message">This room has closed due to inactivity</div>');
            }
        };

        $('#message-area').on('keypress', function(e) {
            if(e.which == 13) {
                e.preventDefault();
                ws.send(JSON.stringify({ type: "message", text: $(this).val(), name: localName, color: localColor}));
                $(this).val("");
            }
        });

        $('#make-a-room button').click(function(e) {
            e.preventDefault();
            ws.send(JSON.stringify({ type: "create" }));
            $('#room-info').show(400);
            copyClick();
        });

        $('#join-a-room button').click(function(e) {
            e.preventDefault();
            $('#make-a-room, #join-a-room button').hide(400);
            $('#join-a-room input').show(400);
        });

        $('#join-a-room input').on('keypress', function(e) {
             if(e.which == 13) {
                 e.preventDefault();
                 ws.send(JSON.stringify({ type: "join", room: $(this).val() }));
                 $('#room-info').show(400);
             }
        });
    };

    makeWSConnection(name, color);
});
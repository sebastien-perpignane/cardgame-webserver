var stompClient = null;
gameId = "";

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    var socket = new SockJS('/stomp');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/game', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });
    });
}

function startNewGame() {

    var socket = new SockJS('/stomp');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {



        setConnected(true);
        console.log('Connected: ' + frame);
        /*stompClient.subscribe('/topic/game', function (greeting) {
            showGreeting(JSON.parse(greeting.body).content);
        });*/

        $.ajax({
            url : 'http://localhost:8080/contree/game/create',
            type : 'POST',
            //dataType:'json',
            success : function(data) {
                gameId = data;
                console.log("Data : " + data);
                console.log("Game Id : " + gameId);

                stompClient.subscribe('/topic/game/' + gameId, function(greeting) {
                    showGreeting(greeting.body);
                });

                $.ajax({
                    url: 'http://localhost:8080/contree/game/' + gameId + '/join',
                    type: 'POST',
                    //dataType:'json',
                    success: function (data) {
                        console.log("Join data:" + data);
                    },
                    error: function (request, error) {
                        alert("Request: " + JSON.stringify(request));
                    }
                });

            },
            error : function(request,error)
            {
                alert("Request: "+JSON.stringify(request));
            }
        });



    });
}

function disconnect() {
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    stompClient.send("/app/hello", {}, JSON.stringify({'name': $("#name").val()}));
}

function showGreeting(message) {
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

function subscribe() {
    stompClient.subscribe('/topic/game/' + $("#game-id").val(), function(greeting) {
        //alert(greeting);
        showGreeting(greeting.body);
    });
}

function placeBid() {
    $.ajax({

        url : 'http://localhost:8080/contree/game/' +  gameId + '/place-bid',
        type : 'POST',
        data : JSON.stringify({
            'playerName' : 'Seb',
            'bidValue': $('#bid-value').val(),
            'cardSuit': $('#bid-suit').val()
        }),
        contentType: 'application/json; charset=UTF-8',
        success : function(data) {
            console.log('Place bid data: '+data);
        },
        error : function(request,error)
        {
            alert("Request: "+JSON.stringify(request));
        }
    });
}

function playCard() {
    $.ajax({

        url : 'http://localhost:8080/contree/game/' + gameId + '/play-card',
        type : 'POST',
        data : JSON.stringify({
            'playerName' : 'Seb',
            'card': $('#played-card').val()
        }),
        contentType: 'application/json; charset=UTF-8',
        success : function(data) {
            console.log('Play card data: '+data);
        },
        error : function(request,error)
        {
            alert("Request: "+JSON.stringify(request));
        }
    });
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#connect" ).click(function() { startNewGame(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendName(); });
    $( "#subscribe" ).click(function() { subscribe(); });
    $( "#place-bid" ).click(function() { placeBid(); });
    $( "#play-card" ).click(function() { playCard(); });
});

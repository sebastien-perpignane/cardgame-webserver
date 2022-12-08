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
    //stompClient.debug = function(str) {};
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/game', function (greeting) {
            displayPlayerMessage(JSON.parse(greeting.body).content);
        });
    });
}

function startNewGame() {

    var socket = new SockJS('/stomp');
    stompClient = Stomp.over(socket);
    //stompClient.debug = function(str) {};
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        /*stompClient.subscribe('/topic/game', function (greeting) {
            displayPlayerMessage(JSON.parse(greeting.body).content);
        });*/

        $.ajax({
            url : 'http://localhost:8080/contree/game/create',
            type : 'POST',
            //dataType:'json',
            success : function(data) {
                gameId = data;
                console.log("Data : " + data);
                console.log("Game Id : " + gameId);

                stompClient.subscribe('/topic/game/' + gameId, function(message) {
                    let event = JSON.parse(message.body);
                    let eventData = event.eventData;

                    if (typeof eventData === 'string' || eventData instanceof String) {
                        //eventData = JSON.stringify(eventData);
                        displayPlayerMessage(eventData);
                    }
                    else {
                        switch(event.type) {
                            case 'PLAY_TURN':
                                managePlayTurn(event)
                                break
                            case 'BID_TURN':
                                manageBidTurn(event)
                                break
                            case 'PLACED_BID':
                                managePlacedBid(event);
                                break;
                            default:
                                let eventDataAsStr = JSON.stringify(eventData);
                                displayPlayerMessage(eventDataAsStr);
                        }
                    }

                });

                $.ajax({
                    url: 'http://localhost:8080/contree/game/' + gameId + '/join',
                    type: 'POST',
                    //dataType:'json',
                    data: {playerName : $('#player-name').val()},
                    success: function (data) {
                        console.log("Join data:" + data);
                    },
                    error: function (request) {
                        alert("Request: " + JSON.stringify(request));
                    }
                });

            },
            error : function(request)
            {
                alert("Request: "+JSON.stringify(request));
            }
        });



    });
}

function managePlacedBid(event) {
    let eventData = event.eventData
    console.log("managePlacedBid event data : " + JSON.stringify(eventData))
    let myPlayer = eventData.player
    let bidValue = eventData.bidValue.display
    let cardSuit = eventData.cardSuit

    let placedBidMessage = myPlayer.name + '(' + myPlayer.team + ') bids ' + bidValue + ' ' + (cardSuit === null ? '' : cardSuit);
    console.log('placedBidMessage: ' + placedBidMessage)

    displayPlayerMessage(placedBidMessage);

}

function managePlayTurn(event) {

    $('#play-form').show();
    $('#bid-form').hide();

    let eventData = event.eventData
    let playCardSelect = document.getElementById('played-card');
    playCardSelect.innerHTML = "";
    for (let index = 0; index < eventData.allowedCards.length; ++index) {
        const element = eventData.allowedCards[index];
        let opt = document.createElement('option');
        opt.value = element.name;
        opt.innerHTML = element.display;
        playCardSelect.appendChild(opt);
    }

    displayHand(eventData.hand)

    /*let displayHand = eventData.hand.map((c) => {
        return c.display
    });

    $('#hand').val(displayHand);*/
}

function displayHand(hand) {
    let displayHand = hand.map((c) => {
        return c.display
    });

    $('#hand').val(displayHand);
}

function manageBidTurn(event) {

    $('#play-form').hide();
    $('#bid-form').show();

    let eventData = event.eventData
    let bidValueSelect = document.getElementById('bid-value');
    bidValueSelect.innerHTML = "";
    for (let index = 0; index < eventData.allowedBidValues.length; ++index) {
        const element = eventData.allowedBidValues[index];
        let opt = document.createElement('option');
        opt.value = element.name;
        opt.innerText = element.display;
        bidValueSelect.appendChild(opt);
    }

    displayHand(eventData.hand);

    //$('#hand').val(JSON.stringify(eventData.hand));

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

function displayPlayerMessage(message) {
    $("#messages").append("<tr><td>" + message + "</td></tr>");
}

function subscribe() {
    stompClient.subscribe('/topic/game/' + $("#game-id").val(), function(greeting) {
        //alert(greeting);
        displayPlayerMessage(greeting.body);
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
        error : function(request)
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
        error : function(request)
        {
            alert("Request: "+JSON.stringify(request));
        }
    });
}

$(function () {
    $("form").on('submit', function (e) {
        e.preventDefault();
    });
    $( "#start-new-game" ).click(function() { startNewGame(); });
    $( "#disconnect" ).click(function() { disconnect(); });
    $( "#send" ).click(function() { sendName(); });
    $( "#subscribe" ).click(function() { subscribe(); });
    $( "#place-bid" ).click(function() { placeBid(); });
    $( "#play-card" ).click(function() { playCard(); });
});

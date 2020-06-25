module Util.Html exposing
    ( KeyCode(..)
    , checkbox
    , classActive
    , intToKeyCode
    , onClickk
    , onDragEnter
    , onDragLeave
    , onDragOver
    , onDropFiles
    , onKeyUp
    , onKeyUpCode
    )

import File exposing (File)
import Html exposing (Attribute, Html, i)
import Html.Attributes exposing (class)
import Html.Events exposing (keyCode, on, preventDefaultOn)
import Json.Decode as D


checkboxChecked : Html msg
checkboxChecked =
    i [ class "ui check square outline icon" ] []


checkboxUnchecked : Html msg
checkboxUnchecked =
    i [ class "ui square outline icon" ] []


checkbox : Bool -> Html msg
checkbox flag =
    if flag then
        checkboxChecked

    else
        checkboxUnchecked


type KeyCode
    = Up
    | Down
    | Left
    | Right
    | Enter
    | Space


intToKeyCode : Int -> Maybe KeyCode
intToKeyCode code =
    case code of
        38 ->
            Just Up

        40 ->
            Just Down

        39 ->
            Just Right

        37 ->
            Just Left

        13 ->
            Just Enter

        32 ->
            Just Space

        _ ->
            Nothing


onKeyUp : (Int -> msg) -> Attribute msg
onKeyUp tagger =
    on "keyup" (D.map tagger keyCode)


onKeyUpCode : (Maybe KeyCode -> msg) -> Attribute msg
onKeyUpCode tagger =
    onKeyUp (intToKeyCode >> tagger)


onClickk : msg -> Attribute msg
onClickk msg =
    Html.Events.preventDefaultOn "click" (D.map alwaysPreventDefault (D.succeed msg))


alwaysPreventDefault : msg -> ( msg, Bool )
alwaysPreventDefault msg =
    ( msg, True )


classActive : Bool -> String -> Attribute msg
classActive active classes =
    class
        (classes
            ++ (if active then
                    " active"

                else
                    ""
               )
        )


onDragEnter : msg -> Attribute msg
onDragEnter m =
    hijackOn "dragenter" (D.succeed m)


onDragOver : msg -> Attribute msg
onDragOver m =
    hijackOn "dragover" (D.succeed m)


onDragLeave : msg -> Attribute msg
onDragLeave m =
    hijackOn "dragleave" (D.succeed m)


onDrop : msg -> Attribute msg
onDrop m =
    hijackOn "drop" (D.succeed m)


onDropFiles : (File -> List File -> msg) -> Attribute msg
onDropFiles tagger =
    let
        dropFilesDecoder =
            D.at [ "dataTransfer", "files" ] (D.oneOrMore tagger File.decoder)
    in
    hijackOn "drop" dropFilesDecoder


hijackOn : String -> D.Decoder msg -> Attribute msg
hijackOn event decoder =
    preventDefaultOn event (D.map hijack decoder)


hijack : msg -> ( msg, Bool )
hijack msg =
    ( msg, True )

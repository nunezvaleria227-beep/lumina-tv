sub init()
    m.userBox = m.top.findNode("userBox")
    m.passBox = m.top.findNode("passBox")
    m.userIcon = m.top.findNode("userIcon")
    m.passIcon = m.top.findNode("passIcon")
    m.userText = m.top.findNode("userText")
    m.passText = m.top.findNode("passText")
    m.btn = m.top.findNode("btn")
    m.btnText = m.top.findNode("btnText")
    m.btnArrow = m.top.findNode("btnArrow")
    m.idx = 0
    m.errorLabel = m.top.findNode("error")
    m.status = m.top.findNode("status")
    m.lastTry = m.top.findNode("lastTry")
    m.autoTimer = m.top.findNode("autoTimer")
    m.tasks = []
    m.busy = false
    ' "manual" = la persona pulsó Entrar (no fue la entrada automática con la sesión guardada).
    m.manual = false
    m.dbgUser = "-"
    m.dbgPass = "-"
    m.user = regRead("user")
    m.pass = regRead("pass")
    ' Si el intento anterior no funcionó, se deja a la vista el motivo (ayuda a corregir y a pedir soporte).
    m.lastTry.text = regRead("lastTry")

    if m.user <> "" and m.pass <> "" then m.idx = 2
    buildDevices()
    refreshList()

    ' Si ya había una sesión guardada, entra sola.
    if m.user <> "" and m.pass <> ""
        m.status.text = "Abriendo…"
        m.autoTimer.observeField("fire", "onAutoLogin")
        m.autoTimer.control = "start"
    end if
end sub

sub focusScreen()
    m.top.setFocus(true)
    refreshList()
end sub

sub refreshList()
    if m.user = ""
        m.userText.text = "Escribe tu usuario"
        m.userText.color = "0x8E7A8BFF"
    else
        m.userText.text = m.user
        m.userText.color = "0xFBF3F9FF"
    end if
    if m.pass = ""
        m.passText.text = "Escribe tu contraseña"
        m.passText.color = "0x8E7A8BFF"
    else
        m.passText.text = m.pass
        m.passText.color = "0xFBF3F9FF"
    end if
    paintFocus()
end sub

' El campo enfocado se pinta rosa; el botón Entrar enfocado lleva borde blanco.
sub paintFocus()
    if m.idx = 0
        m.userBox.uri = "pkg:/images/field_on.9.png"
        m.userIcon.blendColor = "0xFF9ED0FF"
    else
        m.userBox.uri = "pkg:/images/field.9.png"
        m.userIcon.blendColor = "0xCDB8C9FF"
    end if
    if m.idx = 1
        m.passBox.uri = "pkg:/images/field_on.9.png"
        m.passIcon.blendColor = "0xFF9ED0FF"
    else
        m.passBox.uri = "pkg:/images/field.9.png"
        m.passIcon.blendColor = "0xCDB8C9FF"
    end if
    if m.idx = 2 then m.btn.uri = "pkg:/images/btn_enter_on.png" else m.btn.uri = "pkg:/images/btn_enter.png"
    w = m.btnText.boundingRect().width
    m.btnArrow.translation = [640 + Int(w / 2) + 8, 418]
end sub

' "✦ Smart TV  ✦ Fire TV Stick  ✦ Android TV  ✦ Roku" centrado debajo del botón.
sub buildDevices()
    row = m.top.findNode("devices")
    names = ["Smart TV", "Fire TV Stick", "Android TV", "Roku"]
    x = 0
    for each n in names
        st = row.createChild("Poster")
        st.uri = "pkg:/images/star4.png"
        st.width = 11
        st.height = 11
        st.translation = [x, 5]
        lb = row.createChild("Label")
        f = CreateObject("roSGNode", "Font")
        f.uri = "pkg:/fonts/Poppins-Medium.ttf"
        f.size = 13
        lb.font = f
        lb.text = n
        lb.color = "0xA88FA4FF"
        lb.translation = [x + 17, 0]
        x = x + 17 + lb.boundingRect().width + 24
    end for
    x = x - 24
    row.translation = [640 - Int(x / 2), 516]
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    if key = "up"
        if m.idx > 0 then m.idx = m.idx - 1
        paintFocus()
        return true
    end if
    if key = "down"
        if m.idx < 2 then m.idx = m.idx + 1
        paintFocus()
        return true
    end if
    if key = "OK"
        onSelected(m.idx)
        return true
    end if
    return false
end function

' Con una sesión guardada se abre el inicio al instante; la cuenta se vuelve a comprobar en segundo plano.
sub onAutoLogin()
    liveExt = regRead("liveExt")
    if liveExt = "" then liveExt = "m3u8"
    account = invalid
    raw = regRead("account")
    if raw <> ""
        a = ParseJson(raw)
        if a <> invalid and Type(a) = "roAssociativeArray"
            account = { exp: toI(a.exp), maxCon: toI(a.maxCon), activeCon: toI(a.activeCon), status: toS(a.status), user: toS(a.user) }
        end if
    end if
    m.top.event = { type: "loggedIn", user: m.user, pass: m.pass, account: account, liveExt: liveExt, base: regRead("base"), manual: false, restored: true }
end sub

sub onSelected(index as Integer)
    if index = 0
        askText("Usuario", m.user, false, "user")
    else if index = 1
        askText("Contraseña", m.pass, false, "pass")
    else
        m.manual = true
        submit()
    end if
end sub

sub askText(titleText as String, current as String, secure as Boolean, which as String)
    dlg = CreateObject("roSGNode", "StandardKeyboardDialog")
    dlg.title = titleText
    dlg.text = current
    ' Un solo botón: Atrás también cierra el teclado y lo escrito se conserva (así también se puede llenar con pruebas automáticas).
    dlg.buttons = ["Aceptar"]
    if secure and dlg.textEditBox <> invalid then dlg.textEditBox.secureMode = true
    m.dialog = dlg
    m.which = which
    ' El texto se va guardando mientras se escribe.
    dlg.observeFieldScoped("text", "onKeyboardText")
    if dlg.textEditBox <> invalid then dlg.textEditBox.observeFieldScoped("text", "onKeyboardText")
    dlg.observeFieldScoped("buttonSelected", "onKeyboardDone")
    dlg.observeFieldScoped("wasClosed", "onKeyboardClosed")
    m.top.getScene().dialog = dlg
end sub

' Lee lo escrito en el teclado (de la caja de texto) y lo guarda. También anota cuántos caracteres veía cada fuente.
sub readKeyboard()
    dlg = m.dialog
    if dlg = invalid then return
    value = toS(dlg.text)
    dlgLen = Len(value)
    boxLen = -1
    editBox = dlg.textEditBox
    if editBox <> invalid
        boxText = editBox.text
        if boxText <> invalid
            value = boxText
            boxLen = Len(boxText)
        end if
    end if
    note = boxLen.ToStr() + "/" + dlgLen.ToStr()
    if m.which = "user"
        m.user = value.Trim()
        m.dbgUser = note
    else
        m.pass = value
        m.dbgPass = note
    end if
end sub

sub onKeyboardText()
    readKeyboard()
end sub

sub onKeyboardDone()
    dlg = m.dialog
    if dlg = invalid then return
    readKeyboard()
    m.dialog = invalid
    dlg.close = true
    refreshList()
    m.top.setFocus(true)
end sub

sub onKeyboardClosed()
    if m.dialog <> invalid
        readKeyboard()
        m.dialog = invalid
    end if
    refreshList()
    m.top.setFocus(true)
end sub

sub setError(text as String)
    m.errorLabel.text = text
end sub

sub submit()
    if m.busy then return
    if m.user = "" or m.pass = ""
        m.status.text = ""
        setError("Escribe tu usuario y tu contraseña.")
        return
    end if
    m.busy = true
    setError("")
    ' Se guarda enseguida: si la app se cierra mientras el servidor contesta, al abrirla de nuevo entra sola con
    ' esta cuenta y la comprueba en segundo plano. Si el servidor la rechaza, se borra más abajo.
    regWrite("user", m.user)
    regWrite("pass", m.pass)
    m.status.text = "Entrando… un momento"
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { action: "login", user: m.user, pass: m.pass }
    t.observeField("response", "onLogin")
    m.tasks.Push(t)
    t.control = "RUN"
end sub

sub onLogin(event as Object)
    resp = event.getData()
    m.busy = false
    m.status.text = ""
    manual = m.manual
    m.manual = false
    if resp.ok = true
        if regRead("lastTry") <> "" then regDelete("lastTry")
        m.lastTry.text = ""
        m.top.event = { type: "loggedIn", user: m.user, pass: m.pass, account: resp.account, liveExt: resp.liveExt, base: resp.base, manual: manual, restored: false }
    else
        setError(resp.error)
        ' No funcionó: no queda guardada una cuenta sin comprobar.
        regDelete("user")
        regDelete("pass")
        userLen = Len(m.user)
        passLen = Len(m.pass)
        diag = toS(resp.diag) + ". Usuario " + Chr(34) + m.user + Chr(34) + " (" + userLen.ToStr() + "), contraseña " + passLen.ToStr() + ". Teclado u " + m.dbgUser + ", c " + m.dbgPass
        diag = "Último intento: " + diag
        regWrite("lastTry", diag)
        m.lastTry.text = diag
        m.top.setFocus(true)
        m.top.event = { type: "loginFailed" }
    end if
end sub

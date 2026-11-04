sub init()
    m.cover = m.top.findNode("cover")
    m.name = m.top.findNode("name")
    m.meta = m.top.findNode("meta")
    m.plot = m.top.findNode("plot")
    m.extra = m.top.findNode("extra")
    m.actions = m.top.findNode("actions")
    m.status = m.top.findNode("status")
    m.idTag = m.top.findNode("idTag")
    m.autoplayDone = false
    m.tasks = []
    m.ext = "mp4"
    m.actionIds = []
    m.actions.observeField("itemSelected", "onAction")
end sub

sub focusScreen()
    buildActions()
    m.actions.setFocus(true)
end sub

sub onItem()
    it = m.top.item
    m.name.text = toS(it.name)
    m.cover.uri = toS(it.icon)
    ' Número de identificación de la película en el portal (sirve para registrar enlaces directos y para soporte).
    m.idTag.text = "ID de la película: " + toS(it.id)
    m.ext = toS(it.ext)
    if m.ext = "" then m.ext = "mp4"
    m.meta.text = ""
    m.plot.text = ""
    m.extra.text = ""
    m.status.text = "Cargando información…"
    buildActions()
    layout()
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { action: "vodinfo", id: toS(it.id), tag: toS(it.id) }
    t.observeField("response", "onInfo")
    m.tasks.Push(t)
    t.control = "RUN"
end sub

sub onInfo(event as Object)
    resp = event.getData()
    m.status.text = ""
    if resp.ok <> true
        m.plot.text = "No se pudo cargar la descripción, pero la película se puede reproducir."
        maybeAutoplay()
        return
    end if
    info = resp.info
    if m.name.text = "" and info.name <> "" then m.name.text = info.name
    parts = []
    if info.year <> "" then parts.Push(info.year)
    if info.duration <> "" then parts.Push(info.duration)
    if info.genre <> "" then parts.Push(info.genre)
    if info.rating <> "" and info.rating <> "0" then parts.Push("Puntuación " + info.rating)
    m.meta.text = joinParts(parts)
    g = info.genre.Split(",")
    if info.genre <> "" then m.top.findNode("kicker").text = "PELÍCULA  ·  " + UCase(g[0].Trim())
    m.plot.text = info.plot
    extra = ""
    if info.director <> "" then extra = "Dirección: " + info.director
    if info.cast <> ""
        if extra <> "" then extra = extra + Chr(10)
        extra = extra + "Reparto: " + info.cast
    end if
    m.extra.text = extra
    if toS(info.backdrop) <> "" then m.top.findNode("backdrop").uri = toS(info.backdrop)
    if info.cover <> "" and toS(m.top.item.icon) = "" then m.cover.uri = info.cover
    if info.ext <> "" then m.ext = info.ext
    layout()
    maybeAutoplay()
end sub

' Si se llegó por un enlace directo, la película empieza sola en cuanto se sabe su formato.
sub maybeAutoplay()
    if m.autoplayDone then return
    it = m.top.item
    if it = invalid then return
    ' Solo las películas abiertas desde un enlace directo traen esta marca; las demás no la tienen.
    if not it.DoesExist("autoplay") then return
    if it.autoplay <> true then return
    m.autoplayDone = true
    item = { id: toS(it.id), name: m.name.text, icon: toS(it.icon), ext: m.ext }
    saved = findContinue("movie", item.id)
    secs = 0
    if saved <> invalid and toI(saved.pos) >= 5 then secs = toI(saved.pos)
    m.top.event = { type: "play", mode: "movie", items: [item], index: 0, start: secs }
end sub

' Acomoda datos, sinopsis, créditos y botones debajo del título según cuánto ocupa cada texto.
sub layout()
    nameH = m.name.boundingRect().height
    if nameH < 46 then nameH = 46
    y = 114 + nameH + 4
    m.meta.translation = [282, y]
    m.status.translation = [282, y]
    y = y + 32
    m.plot.translation = [282, y]
    if m.plot.text <> "" then y = y + m.plot.boundingRect().height + 10
    m.extra.translation = [282, y]
    if m.extra.text <> "" then y = y + m.extra.boundingRect().height + 12
    if y < 330 then y = 330
    if y > 470 then y = 470
    m.actions.translation = [282, y]
    m.top.findNode("progBg").translation = [282, y + 60]
    m.top.findNode("progBar").translation = [282, y + 60]
    m.top.findNode("progLabel").translation = [514, y + 52]
end sub

function joinParts(parts as Object) as String
    out = ""
    for each p in parts
        if out <> "" then out = out + "   •   "
        out = out + p
    end for
    return out
end function

sub buildActions()
    it = m.top.item
    if it = invalid then return
    root = CreateObject("roSGNode", "ContentNode")
    m.actionIds = []
    saved = findContinue("movie", toS(it.id))
    progBg = m.top.findNode("progBg")
    progBar = m.top.findNode("progBar")
    progLabel = m.top.findNode("progLabel")
    if saved <> invalid and toI(saved.pos) >= 5
        addAction(root, "resume", "Continuar " + fmtClock(toI(saved.pos)), "ic_play")
        addAction(root, "start", "Desde el inicio", "")
        dur = toI(saved.dur)
        if dur > 0
            w = Int(220 * toI(saved.pos) / dur)
            if w < 4 then w = 4
            if w > 220 then w = 220
            progBar.width = w
            progBg.visible = true
            progBar.visible = true
            progLabel.text = "Te quedaste en " + fmtClock(toI(saved.pos))
            progLabel.visible = true
        end if
    else
        addAction(root, "start", "Ver ahora", "ic_play")
        progBg.visible = false
        progBar.visible = false
        progLabel.visible = false
    end if
    if isFavorite("movie", toS(it.id))
        addAction(root, "fav", "En mi lista", "ic_check")
    else
        addAction(root, "fav", "Mi lista", "ic_plus")
    end if
    m.actions.content = root
end sub

sub addAction(root as Object, id as String, title as String, icon as String)
    n = root.CreateChild("ContentNode")
    n.title = title
    if icon <> "" then n.HDPosterUrl = "pkg:/images/" + icon + ".png"
    m.actionIds.Push(id)
end sub

sub onAction(event as Object)
    index = event.getData()
    action = m.actionIds[index]
    it = m.top.item
    item = { id: toS(it.id), name: m.name.text, icon: toS(it.icon), ext: m.ext }
    if action = "start"
        m.top.event = { type: "play", mode: "movie", items: [item], index: 0, start: 0 }
    else if action = "resume"
        saved = findContinue("movie", item.id)
        secs = 0
        if saved <> invalid then secs = toI(saved.pos)
        m.top.event = { type: "play", mode: "movie", items: [item], index: 0, start: secs }
    else if action = "fav"
        toggleFavorite("movie", item)
        buildActions()
        m.actions.setFocus(true)
    end if
end sub

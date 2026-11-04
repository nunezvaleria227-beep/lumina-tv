' Inicio de Lumina TV: carrusel de portadas, fondo con los colores de la portada, botones y barra flotante.
' Zonas del foco: "head" (nombre de la fila), "covers" (carrusel), "buttons" (Ver ahora…), "dock" (barra de abajo).

sub init()
    m.stage = m.top.findNode("stage")
    m.anim = m.top.findNode("anim")
    m.fade = m.top.findNode("fade")
    m.fadeIn = m.top.findNode("fadeIn")
    m.fadeOut = m.top.findNode("fadeOut")
    m.ambA = m.top.findNode("ambA")
    m.ambB = m.top.findNode("ambB")
    m.clock = m.top.findNode("clock")
    m.dateLabel = m.top.findNode("dateLabel")
    m.headHint = m.top.findNode("headHint")
    m.head = m.top.findNode("head")
    m.headChip = m.top.findNode("headChip")
    m.headStar = m.top.findNode("headStar")
    m.headTitle = m.top.findNode("headTitle")
    m.status = m.top.findNode("status")
    m.title = m.top.findNode("title")
    m.meta = m.top.findNode("meta")
    m.btns = m.top.findNode("btns")
    m.shelfList = m.top.findNode("shelfList")
    m.dock = m.top.findNode("dock")
    m.infoTimer = m.top.findNode("infoTimer")
    m.clockTimer = m.top.findNode("clockTimer")

    m.shelves = []
    m.shelfKey = "new"
    m.index = 0
    m.cards = []
    m.cardSig = ""
    m.zone = "covers"
    m.server = invalid
    m.loading = false
    m.loadError = invalid
    m.infoCache = {}
    m.infoTasks = {}
    m.pendingInfo = invalid
    m.shown = invalid
    m.ambUri = ""
    m.ambFront = "A"
    m.btnIds = []
    m.homeTask = invalid

    m.btns.observeField("itemSelected", "onButton")
    m.btns.observeField("rowWidth", "centerButtons")
    m.dock.observeField("selected", "onDock")
    m.infoTimer.observeField("fire", "onInfoTimer")
    m.clockTimer.observeField("fire", "refreshClock")
    m.ambA.observeField("loadStatus", "onAmbLoaded")
    m.ambB.observeField("loadStatus", "onAmbLoaded")
    m.clockTimer.control = "start"
    refreshClock()
    buildShelves()
    loadHome()
end sub

sub focusScreen()
    refreshClock()
    ' Al volver de otra pantalla se actualizan "Continuar viendo", "Mi lista" y los canales vistos.
    buildShelves()
    applyFocus()
end sub

sub refreshClock()
    now = nowSecs()
    m.clock.text = fmtHM(now)
    m.dateLabel.text = fmtWeekDate(now)
end sub

' La barra de abajo de otra pantalla pidió "Mi lista": se abre esa fila.
sub onGoShelf()
    key = m.top.goShelf
    if key = "" then return
    m.shelfKey = key
    m.index = 0
    m.cardSig = ""
    buildShelves()
    setZone("covers")
end sub

' ----------------------------------------------------------------- datos del servidor

sub loadHome()
    cached = cacheGet("home_rows", 900)
    if cached <> invalid
        onHomeData(cached.data)
        return
    end if
    m.loading = true
    buildShelves()
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { action: "home", tag: "home" }
    t.observeField("response", "onHomeLoaded")
    m.homeTask = t
    t.control = "RUN"
end sub

sub onHomeLoaded(event as Object)
    resp = event.getData()
    m.homeTask = invalid
    m.loading = false
    if resp = invalid or resp.ok <> true
        msg = "No se pudo cargar el inicio. Revisa tu internet e inténtalo otra vez."
        if resp <> invalid and toS(resp.error) <> "" then msg = toS(resp.error)
        m.loadError = msg
        buildShelves()
        return
    end if
    data = { movies: resp.movies, series: resp.series, live: resp.live, liveName: toS(resp.liveName) }
    holder = CreateObject("roSGNode", "Node")
    holder.addFields({ data: data })
    cachePut("home_rows", holder)
    onHomeData(data)
end sub

sub onHomeData(data as Object)
    m.loading = false
    m.server = data
    buildShelves()
end sub

' ----------------------------------------------------------------- filas

sub buildShelves()
    shelves = []

    cont = []
    for each c in loadList("continue")
        what = toS(c.kind)
        pct = 0.0
        if toI(c.dur) > 0 then pct = toI(c.pos) / toI(c.dur)
        cont.Push({ kind: "continue", what: what, id: toS(c.id), name: toS(c.name), icon: toS(c.icon), ext: toS(c.ext), pos: toI(c.pos), dur: toI(c.dur), pct: pct, raw: c })
    end for
    if cont.Count() > 0 then shelves.Push({ key: "resume", title: "Continuar viendo", items: cont })

    if m.server <> invalid
        if m.server.movies.Count() > 0 then shelves.Push({ key: "new", title: "Estrenos", items: m.server.movies })
        if m.server.series.Count() > 0 then shelves.Push({ key: "series", title: "Series nuevas", items: m.server.series })
    end if

    live = []
    for each c in recentLive()
        live.Push({ kind: "live", id: toS(c.id), name: toS(c.name), icon: toS(c.icon) })
    end for
    if live.Count() = 0 and m.server <> invalid then live = m.server.live
    if live.Count() > 0 then shelves.Push({ key: "live", title: "En vivo", items: live })

    mine = []
    for each f in loadList(favKey("movie"))
        mine.Push({ kind: "movie", id: toS(f.id), name: toS(f.name), icon: toS(f.icon), ext: toS(f.ext) })
    end for
    for each f in loadList(favKey("series"))
        mine.Push({ kind: "series", id: toS(f.id), name: toS(f.name), icon: toS(f.icon) })
    end for
    if mine.Count() > 0 then shelves.Push({ key: "mine", title: "Mi lista", items: mine })

    m.shelves = shelves
    if shelves.Count() = 0
        clearCards()
        if m.loading
            m.status.text = "Cargando lo nuevo…"
        else if m.loadError <> invalid
            m.status.text = m.loadError
        else
            m.status.text = "Aún no hay películas ni series. Entra a En vivo, Películas o Series desde la barra de abajo."
        end if
        paintHead()
        paintShelfList()
        showCurrent()
        return
    end if
    m.status.text = ""
    si = shelfIndex()
    m.shelfKey = shelves[si].key
    items = shelves[si].items
    sig = m.shelfKey + "|" + items.Count().ToStr()
    if items.Count() > 0 then sig = sig + "|" + toS(items[0].id) + "|" + toS(items[items.Count() - 1].id)
    if sig <> m.cardSig
        m.cardSig = sig
        buildCards()
    end if
    paintHead()
    paintShelfList()
    showCurrent()
end sub

function shelfIndex() as Integer
    for i = 0 to m.shelves.Count() - 1
        if m.shelves[i].key = m.shelfKey then return i
    end for
    for i = 0 to m.shelves.Count() - 1
        if m.shelves[i].key = "new" then return i
    end for
    return 0
end function

function currentItems() as Object
    if m.shelves.Count() = 0 then return []
    return m.shelves[shelfIndex()].items
end function

function currentItem() as Dynamic
    items = currentItems()
    if m.index < 0 or m.index >= items.Count() or m.index >= m.cards.Count() then return invalid
    return items[m.index]
end function

sub changeShelf(stepBy as Integer)
    if m.shelves.Count() < 2 then return
    si = shelfIndex() + stepBy
    if si < 0 or si >= m.shelves.Count() then return
    m.shelfKey = m.shelves[si].key
    m.index = 0
    buildShelves()
end sub

' ----------------------------------------------------------------- carrusel

sub clearCards()
    m.anim.control = "stop"
    m.stage.removeChildrenIndex(m.stage.getChildCount(), 0)
    m.cards = []
    m.cardSig = ""
end sub

sub buildCards()
    clearCards()
    m.cardSig = m.shelfKey
    items = currentItems()
    n = items.Count()
    if n > 40 then n = 40
    if m.index >= n then m.index = n - 1
    if m.index < 0 then m.index = 0
    for i = 0 to n - 1
        m.cards.Push(makeCard(items[i], i))
    end for
    ' Entra con un fundido para que se note el cambio de fila.
    for each c in m.cards
        c.g.opacity = 0
    end for
    placeAll(true)
end sub

function makeFont(file as String, size as Integer) as Object
    f = CreateObject("roSGNode", "Font")
    f.uri = "pkg:/fonts/" + file
    f.size = size
    return f
end function

function makeCard(e as Object, i as Integer) as Object
    g = m.stage.createChild("Group")
    g.id = "cv" + i.ToStr()
    kind = e.kind
    if kind = "continue" then kind = e.what

    bg = g.createChild("Poster")
    bg.uri = "pkg:/images/card_bg.9.png"
    bg.translation = [-100, -150]
    bg.width = 200
    bg.height = 300

    mg = g.createChild("MaskGroup")
    mg.maskUri = "pkg:/images/card_mask.png"
    mg.maskSize = [200, 300]
    mg.translation = [-100, -150]

    img = mg.createChild("Poster")
    if kind = "live"
        img.loadDisplayMode = "scaleToFit"
        img.translation = [30, 70]
        img.width = 140
        img.height = 110
        img.loadWidth = 280
        img.loadHeight = 220
        img.uri = toS(e.icon)
    else
        img.loadDisplayMode = "scaleToZoom"
        img.width = 200
        img.height = 300
        img.loadWidth = 300
        img.loadHeight = 450
        img.uri = thumbUrl(toS(e.icon))
    end if

    shade = mg.createChild("Poster")
    shade.uri = "pkg:/images/card_shade.png"
    shade.translation = [0, 170]
    shade.width = 200
    shade.height = 130

    title = mg.createChild("Label")
    title.translation = [10, 226]
    title.width = 180
    title.height = 64
    title.wrap = true
    title.maxLines = 2
    title.horizAlign = "center"
    title.vertAlign = "bottom"
    title.color = "0xFBF3F9FF"
    title.font = makeFont("Poppins-Bold.ttf", 14)
    title.text = toS(e.name)

    if e.kind = "continue" and toI(e.dur) > 0
        barBg = mg.createChild("Rectangle")
        barBg.translation = [0, 294]
        barBg.width = 200
        barBg.height = 6
        barBg.color = "0xFFFFFF55"
        bar = mg.createChild("Rectangle")
        bar.translation = [0, 294]
        w = Int(200 * e.pct)
        if w < 4 then w = 4
        if w > 200 then w = 200
        bar.width = w
        bar.height = 6
        bar.color = "0xF2268AFF"
    end if

    edgeL = mg.createChild("Poster")
    edgeL.uri = "pkg:/images/edge_l.png"
    edgeL.width = 200
    edgeL.height = 300
    edgeL.opacity = 0
    edgeR = mg.createChild("Poster")
    edgeR.uri = "pkg:/images/edge_r.png"
    edgeR.width = 200
    edgeR.height = 300
    edgeR.opacity = 0

    ' Borde blanco con brillo rosa de la portada del centro (encima de la imagen).
    ring = g.createChild("Poster")
    ring.uri = "pkg:/images/cover_ring.9.png"
    ring.translation = [-118, -168]
    ring.width = 236
    ring.height = 336
    ring.visible = false

    return { g: g, ring: ring, shade: shade, title: title, edgeL: edgeL, edgeR: edgeR, placed: false }
end function

' Dónde va una portada según su distancia a la del centro: posición, tamaño, ancho (simula el giro) y transparencia.
function slot(d as Integer) as Object
    a = Abs(d)
    if a > 4 then a = 4
    xs = [0, 200, 357, 490, 600]
    ss = [1.0, 0.833, 0.70, 0.60, 0.52]
    cs = [1.0, 0.86, 0.80, 0.76, 0.72]
    os = [1.0, 0.82, 0.56, 0.30, 0.0]
    x = xs[a]
    if d < 0 then x = 0 - x
    return { x: x, s: ss[a], cx: cs[a], o: os[a] }
end function

sub addInterp(kind as String, field as String, fromV as Object, toV as Object)
    it = m.anim.createChild(kind)
    it.key = [0.0, 1.0]
    it.keyValue = [fromV, toV]
    it.fieldToInterp = field
end sub

sub placeAll(animate as Boolean)
    m.anim.control = "stop"
    m.anim.removeChildrenIndex(m.anim.getChildCount(), 0)
    order = []
    for i = 0 to m.cards.Count() - 1
        c = m.cards[i]
        d = i - m.index
        a = Abs(d)
        if a > 4
            c.g.visible = false
            c.placed = false
        else
            c.g.visible = true
            t = slot(d)
            tr = [t.x, 0]
            sc = [t.s * t.cx, t.s]
            if animate and c.placed
                addInterp("Vector2DFieldInterpolator", c.g.id + ".translation", c.g.translation, tr)
                addInterp("Vector2DFieldInterpolator", c.g.id + ".scale", c.g.scale, sc)
                addInterp("FloatFieldInterpolator", c.g.id + ".opacity", c.g.opacity, t.o)
            else if animate
                c.g.translation = tr
                c.g.scale = sc
                addInterp("FloatFieldInterpolator", c.g.id + ".opacity", 0.0, t.o)
            else
                c.g.translation = tr
                c.g.scale = sc
                c.g.opacity = t.o
            end if
            c.placed = true
            edge = a * 0.55
            if edge > 1 then edge = 1
            if d < 0
                c.edgeR.opacity = edge
                c.edgeL.opacity = 0
            else if d > 0
                c.edgeL.opacity = edge
                c.edgeR.opacity = 0
            else
                c.edgeL.opacity = 0
                c.edgeR.opacity = 0
            end if
            c.shade.visible = (d <> 0)
            c.title.visible = (d <> 0)
            c.ring.visible = (d = 0 and m.zone = "covers")
            order.Push({ a: a, g: c.g })
        end if
    end for
    ' Las más lejanas se dibujan primero; la del centro queda encima de todas.
    order.SortBy("a", "r")
    for each o in order
        m.stage.removeChild(o.g)
        m.stage.appendChild(o.g)
    end for
    if m.anim.getChildCount() > 0 then m.anim.control = "start"
end sub

sub moveCover(stepBy as Integer)
    n = m.cards.Count()
    if n = 0 then return
    target = m.index + stepBy
    if target < 0 or target >= n then return
    m.index = target
    placeAll(true)
    showCurrent()
end sub

' ----------------------------------------------------------------- nombre de la fila y lista de filas

sub paintHead()
    if m.shelves.Count() = 0
        m.head.visible = false
        m.headHint.text = ""
        return
    end if
    m.head.visible = true
    si = shelfIndex()
    m.headTitle.text = UCase(m.shelves[si].title)
    w = m.headTitle.boundingRect().width
    total = 20 + w
    if m.zone = "head"
        chipW = total + 36
        x0 = 640 - chipW / 2
        m.headChip.visible = true
        m.headChip.width = chipW
        m.headChip.translation = [x0, 0]
        m.headStar.translation = [x0 + 18, 8]
        m.headTitle.translation = [x0 + 38, 0]
        prev = ""
        nxt = ""
        if si > 0 then prev = "‹  " + m.shelves[si - 1].title
        if si < m.shelves.Count() - 1 then nxt = m.shelves[si + 1].title + "  ›"
        sep = ""
        if prev <> "" and nxt <> "" then sep = "       ·       "
        m.headHint.text = prev + sep + nxt
    else
        x0 = 640 - total / 2
        m.headChip.visible = false
        m.headStar.translation = [x0, 8]
        m.headTitle.translation = [x0 + 20, 0]
        if m.shelves.Count() > 1 then m.headHint.text = "Arriba: cambiar fila" else m.headHint.text = ""
    end if
end sub

sub paintShelfList()
    m.shelfList.removeChildrenIndex(m.shelfList.getChildCount(), 0)
    si = shelfIndex()
    y = 0
    for i = 0 to m.shelves.Count() - 1
        lb = m.shelfList.createChild("Label")
        lb.translation = [0, y]
        lb.height = 16
        if i = si
            lb.text = "•  " + m.shelves[i].title
            lb.color = "0xFF9ED0FF"
            lb.font = makeFont("Poppins-Bold.ttf", 11)
        else
            lb.text = m.shelves[i].title
            lb.color = "0x8E7A8BFF"
            lb.font = makeFont("Poppins-Medium.ttf", 11)
        end if
        y = y + 16
    end for
    m.shelfList.translation = [48, 680 - y]
end sub

' ----------------------------------------------------------------- título, datos y botones

sub showCurrent()
    e = currentItem()
    m.shown = e
    if e = invalid
        m.title.text = ""
        m.meta.text = ""
        m.btns.visible = false
        return
    end if
    m.title.text = toS(e.name)
    kind = e.kind
    if kind = "continue"
        line = "Te quedaste en " + fmtClock(toI(e.pos))
        if toI(e.dur) > 0 then line = line + " de " + fmtClock(toI(e.dur))
        m.meta.text = line
        setAmbient(thumbUrl(toS(e.icon)))
        if e.what = "movie" then requestLater(e)
    else if kind = "live"
        m.meta.text = "Canal en vivo"
        setAmbient("")
    else if kind = "series"
        m.meta.text = metaLine(e, invalid, "Serie")
        setAmbient(firstNonEmpty(toS(e.backdrop), thumbUrl(toS(e.icon))))
    else
        cached = m.infoCache[toS(e.id)]
        m.meta.text = metaLine(e, cached, "")
        bd = toS(e.backdrop)
        if cached <> invalid and toS(cached.backdrop) <> "" then bd = toS(cached.backdrop)
        setAmbient(firstNonEmpty(bd, thumbUrl(toS(e.icon))))
        if cached = invalid then requestLater(e)
    end if
    buildButtons(e)
end sub

function firstNonEmpty(a as String, b as String) as String
    if a <> "" then return a
    return b
end function

function metaLine(e as Object, info as Dynamic, first as String) as String
    parts = []
    if first <> "" then parts.Push(first)
    year = toS(e.year)
    genre = toS(e.genre)
    rating = toS(e.rating)
    dur = ""
    if info <> invalid
        if toS(info.year) <> "" then year = toS(info.year)
        if toS(info.genre) <> "" then genre = toS(info.genre)
        if toS(info.rating) <> "" then rating = toS(info.rating)
        dur = toS(info.duration)
    end if
    if Len(year) > 4 then year = Left(year, 4)
    if year <> "" then parts.Push(year)
    if genre <> ""
        g = genre.Split(",")
        genre = g[0].Trim()
        if g.Count() > 1 then genre = genre + ", " + g[1].Trim()
        parts.Push(genre)
    end if
    if dur <> "" then parts.Push(dur)
    if rating <> "" and rating <> "0" and rating <> "0.0" then parts.Push("Puntuación " + rating)
    out = ""
    for each p in parts
        if out <> "" then out = out + "   •   "
        out = out + p
    end for
    return out
end function

sub buildButtons(e as Object)
    root = CreateObject("roSGNode", "ContentNode")
    m.btnIds = []
    kind = e.kind
    if kind = "continue"
        addBtn(root, "play", "Continuar", "ic_play")
        if e.what = "movie" then addBtn(root, "info", "Más información", "")
    else if kind = "live"
        addBtn(root, "play", "Ver canal", "ic_play")
    else if kind = "series"
        addBtn(root, "play", "Ver serie", "ic_play")
        addFavBtn(root, e)
    else
        addBtn(root, "play", "Ver ahora", "ic_play")
        addBtn(root, "info", "Más información", "")
        addFavBtn(root, e)
    end if
    m.btns.content = root
    m.btns.visible = true
    centerButtons()
end sub

sub addBtn(root as Object, id as String, label as String, icon as String)
    n = root.CreateChild("ContentNode")
    n.title = label
    if icon <> "" then n.HDPosterUrl = "pkg:/images/" + icon + ".png"
    m.btnIds.Push(id)
end sub

sub addFavBtn(root as Object, e as Object)
    if isFavorite(e.kind, toS(e.id))
        addBtn(root, "fav", "En mi lista", "ic_check")
    else
        addBtn(root, "fav", "Mi lista", "ic_plus")
    end if
end sub

sub centerButtons()
    m.btns.translation = [640 - Int(m.btns.rowWidth / 2), 532]
end sub

function itemOf(e as Object) as Object
    return { id: toS(e.id), name: toS(e.name), icon: toS(e.icon), ext: toS(e.ext) }
end function

sub onButton(event as Object)
    idx = event.getData()
    if idx < 0 or idx >= m.btnIds.Count() then return
    e = currentItem()
    if e = invalid then return
    id = m.btnIds[idx]
    if id = "play"
        playItem(e)
    else if id = "info"
        if e.kind = "series"
            m.top.event = { type: "openSeries", item: itemOf(e) }
        else
            m.top.event = { type: "openMovie", item: itemOf(e) }
        end if
    else if id = "fav"
        toggleFavorite(e.kind, itemOf(e))
        buildShelves()
        m.btns.jumpToItem = idx
        if m.zone = "buttons" then m.btns.setFocus(true)
    end if
end sub

' OK sobre la portada.
sub selectCover()
    e = currentItem()
    if e = invalid then return
    if e.kind = "movie"
        m.top.event = { type: "openMovie", item: itemOf(e) }
    else if e.kind = "series"
        m.top.event = { type: "openSeries", item: itemOf(e) }
    else
        playItem(e)
    end if
end sub

sub playItem(e as Object)
    kind = e.kind
    if kind = "continue"
        m.top.event = { type: "playEntry", entry: e.raw }
    else if kind = "live"
        items = []
        for each x in currentItems()
            items.Push({ id: toS(x.id), name: toS(x.name), icon: toS(x.icon), ext: "" })
        end for
        m.top.event = { type: "playLive", items: items, index: m.index }
    else if kind = "series"
        m.top.event = { type: "openSeries", item: itemOf(e) }
    else
        item = itemOf(e)
        if item.ext = "" then item.ext = "mp4"
        cached = m.infoCache[item.id]
        if cached <> invalid and toS(cached.ext) <> "" then item.ext = toS(cached.ext)
        saved = findContinue("movie", item.id)
        secs = 0
        if saved <> invalid and toI(saved.pos) >= 5 then secs = toI(saved.pos)
        m.top.event = { type: "play", mode: "movie", items: [item], index: 0, start: secs }
    end if
end sub

' ----------------------------------------------------------------- fondo con los colores de la portada

sub setAmbient(uri as String)
    if uri = m.ambUri then return
    m.ambUri = uri
    if uri = "" then return
    if m.ambFront = "A" then back = m.ambB else back = m.ambA
    if back.uri = uri and back.loadStatus = "ready"
        crossfade(back)
    else
        back.uri = uri
    end if
end sub

sub onAmbLoaded(event as Object)
    if event.getData() <> "ready" then return
    node = event.getRoSGNode()
    if node.uri <> m.ambUri then return
    crossfade(node)
end sub

sub crossfade(node as Object)
    if m.ambFront = "A" then front = m.ambA else front = m.ambB
    if node.isSameNode(front) then return
    m.fade.control = "stop"
    m.fadeIn.fieldToInterp = node.id + ".opacity"
    m.fadeIn.keyValue = [node.opacity, 0.62]
    m.fadeOut.fieldToInterp = front.id + ".opacity"
    m.fadeOut.keyValue = [front.opacity, 0.0]
    m.fade.control = "start"
    if m.ambFront = "A" then m.ambFront = "B" else m.ambFront = "A"
end sub

' ----------------------------------------------------------------- fichas de películas (año, género, duración, imagen de fondo)

sub requestLater(e as Object)
    m.pendingInfo = e
    m.infoTimer.control = "stop"
    m.infoTimer.control = "start"
end sub

sub onInfoTimer()
    e = m.pendingInfo
    m.pendingInfo = invalid
    if e = invalid then return
    if m.shown = invalid or toS(m.shown.id) <> toS(e.id) then return
    requestInfo(e)
end sub

sub requestInfo(e as Object)
    id = toS(e.id)
    if id = "" then return
    if m.infoCache[id] <> invalid then return
    if m.infoTasks.DoesExist(id) then return
    t = CreateObject("roSGNode", "ApiTask")
    t.request = { action: "vodinfo", id: id, tag: id }
    t.observeField("response", "onInfo")
    m.infoTasks[id] = t
    t.control = "RUN"
end sub

sub onInfo(event as Object)
    resp = event.getData()
    if resp = invalid then return
    id = toS(resp.tag)
    m.infoTasks.Delete(id)
    if resp.ok <> true then return
    m.infoCache[id] = resp.info
    e = m.shown
    if e = invalid or toS(e.id) <> id then return
    if e.kind = "movie" then m.meta.text = metaLine(e, resp.info, "")
    if toS(resp.info.backdrop) <> "" then setAmbient(toS(resp.info.backdrop))
end sub

' ----------------------------------------------------------------- barra de abajo

sub onDock()
    target = m.dock.selected
    if target = "home"
        setZone("covers")
    else if target = "mine"
        found = false
        for each s in m.shelves
            if s.key = "mine" then found = true
        end for
        if found
            m.shelfKey = "mine"
            m.index = 0
            buildShelves()
            setZone("covers")
        else
            m.status.text = "Tu lista está vacía. En una película o serie elige «Mi lista» para guardarla aquí."
        end if
    else
        m.top.event = { type: "open", target: target }
    end if
end sub

' ----------------------------------------------------------------- foco y teclas

sub setZone(z as String)
    m.zone = z
    applyFocus()
end sub

sub applyFocus()
    if m.zone = "buttons" and not m.btns.visible then m.zone = "covers"
    if m.zone = "buttons"
        m.btns.setFocus(true)
    else if m.zone = "dock"
        m.dock.setFocus(true)
    else
        m.top.setFocus(true)
    end if
    for i = 0 to m.cards.Count() - 1
        m.cards[i].ring.visible = (i = m.index and m.zone = "covers")
    end for
    paintHead()
end sub

function onKeyEvent(key as String, press as Boolean) as Boolean
    if not press then return false
    z = m.zone
    if key = "back"
        if z <> "covers"
            setZone("covers")
            return true
        end if
        return false
    end if
    if key = "options"
        showOptions()
        return true
    end if
    if z = "head"
        if key = "left"
            changeShelf(-1)
            paintHead()
        else if key = "right"
            changeShelf(1)
            paintHead()
        else if key = "down" or key = "OK"
            setZone("covers")
        end if
        return true
    end if
    if z = "covers"
        if key = "left"
            moveCover(-1)
        else if key = "right"
            moveCover(1)
        else if key = "up"
            if m.shelves.Count() > 0 then setZone("head")
        else if key = "down"
            if m.btns.visible then setZone("buttons") else setZone("dock")
        else if key = "OK"
            selectCover()
        else if key = "fastforward"
            moveCover(5)
        else if key = "rewind"
            moveCover(-5)
        end if
        return true
    end if
    if z = "buttons"
        if key = "up"
            setZone("covers")
            return true
        end if
        if key = "down"
            setZone("dock")
            return true
        end if
        return false
    end if
    if z = "dock"
        if key = "up"
            if m.btns.visible then setZone("buttons") else setZone("covers")
            return true
        end if
        return false
    end if
    return false
end function

' ----------------------------------------------------------------- tecla * (opciones): cerrar sesión

sub showOptions()
    dlg = CreateObject("roSGNode", "Dialog")
    dlg.title = "Opciones"
    s = m.global.session
    if s <> invalid and s.user <> invalid then dlg.message = "Cuenta: " + toS(s.user)
    dlg.buttons = ["Cerrar sesión", "Cancelar"]
    dlg.observeFieldScoped("buttonSelected", "onOptionsChoice")
    m.dialog = dlg
    m.top.getScene().dialog = dlg
end sub

sub onOptionsChoice()
    dlg = m.dialog
    if dlg = invalid then return
    choice = dlg.buttonSelected
    dlg.close = true
    m.dialog = invalid
    applyFocus()
    if choice = 0 then m.top.event = { type: "logout" }
end sub

// Big play button for the product video on the home page.
// Custom JavaScript approved by the user (2026-11-20, see wiki page CiteOlivia).
// Progressive enhancement: without JS the native player with controls stays usable.
(function () {
  document.querySelectorAll("[data-video-player]").forEach(function (root) {
    var video = root.querySelector("video");
    var button = root.querySelector("[data-video-play]");
    var label = root.querySelector("[data-video-play-label]");
    if (!video || !button) {
      return;
    }

    // Clean poster until the first start: big button instead of the control bar.
    video.controls = false;
    button.hidden = false;

    function start() {
      video.controls = true;
      var playing = video.play();
      if (playing && typeof playing.catch === "function") {
        playing.catch(function () {
          // Playback blocked (e.g. browser policy): the control bar stays visible.
          button.hidden = true;
        });
      }
      video.focus();
    }

    button.addEventListener("click", start);
    video.addEventListener("play", function () {
      button.hidden = true;
      video.controls = true;
    });
    video.addEventListener("ended", function () {
      if (document.fullscreenElement && document.exitFullscreen) {
        document.exitFullscreen();
      }
      if (label) {
        label.textContent = "Nochmal ansehen";
      }
      video.controls = false;
      video.load(); // back to the poster (no download thanks to preload="none")
      button.hidden = false;
    });
  });
})();

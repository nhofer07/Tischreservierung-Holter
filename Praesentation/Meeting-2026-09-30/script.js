const slides = Array.from(document.querySelectorAll('.slide'));
const previousButton = document.getElementById('prevButton');
const nextButton = document.getElementById('nextButton');
const counter = document.getElementById('slideCounter');
const progressBar = document.getElementById('progressBar');
const overviewButton = document.getElementById('overviewButton');
const overviewDialog = document.getElementById('overviewDialog');
const closeOverviewButton = document.getElementById('closeOverview');
const overviewGrid = document.getElementById('overviewGrid');

let currentSlide = getInitialSlide();
let touchStartX = 0;

function getInitialSlide() {
  const slideFromHash = Number(window.location.hash.replace('#', ''));
  if (slideFromHash >= 1 && slideFromHash <= slides.length) {
    return slideFromHash - 1;
  }
  return 0;
}

function twoDigits(value) {
  return String(value).padStart(2, '0');
}

function showSlide(index, updateHash = true) {
  currentSlide = Math.max(0, Math.min(index, slides.length - 1));

  slides.forEach((slide, slideIndex) => {
    slide.classList.toggle('is-active', slideIndex === currentSlide);
    slide.setAttribute('aria-hidden', slideIndex === currentSlide ? 'false' : 'true');
    if (slideIndex === currentSlide) {
      slide.scrollTop = 0;
    }
  });

  counter.textContent = `${twoDigits(currentSlide + 1)} / ${twoDigits(slides.length)}`;
  progressBar.style.width = `${((currentSlide + 1) / slides.length) * 100}%`;
  previousButton.disabled = currentSlide === 0;
  nextButton.disabled = currentSlide === slides.length - 1;
  nextButton.textContent = currentSlide === slides.length - 1 ? 'Ende' : 'Weiter →';

  document.querySelectorAll('.overview-slide').forEach((button, buttonIndex) => {
    button.classList.toggle('is-current', buttonIndex === currentSlide);
  });

  if (updateHash) {
    history.replaceState(null, '', `#${currentSlide + 1}`);
  }
}

function nextSlide() {
  if (currentSlide < slides.length - 1) showSlide(currentSlide + 1);
}

function previousSlide() {
  if (currentSlide > 0) showSlide(currentSlide - 1);
}

function buildOverview() {
  slides.forEach((slide, index) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'overview-slide';
    button.innerHTML = `<small>${twoDigits(index + 1)}</small><strong>${slide.dataset.title}</strong>`;
    button.addEventListener('click', () => {
      showSlide(index);
      overviewDialog.close();
    });
    overviewGrid.appendChild(button);
  });
}

previousButton.addEventListener('click', previousSlide);
nextButton.addEventListener('click', nextSlide);
overviewButton.addEventListener('click', () => overviewDialog.showModal());
closeOverviewButton.addEventListener('click', () => overviewDialog.close());

overviewDialog.addEventListener('click', (event) => {
  if (event.target === overviewDialog) overviewDialog.close();
});

document.addEventListener('keydown', (event) => {
  if (overviewDialog.open || event.target.closest('.qa-item')) return;

  if (['ArrowRight', 'PageDown', ' '].includes(event.key)) {
    event.preventDefault();
    nextSlide();
  }
  if (['ArrowLeft', 'PageUp'].includes(event.key)) {
    event.preventDefault();
    previousSlide();
  }
  if (event.key === 'Home') showSlide(0);
  if (event.key === 'End') showSlide(slides.length - 1);
  if (event.key.toLowerCase() === 'o') overviewDialog.showModal();
});

document.addEventListener('touchstart', (event) => {
  touchStartX = event.changedTouches[0].screenX;
}, { passive: true });

document.addEventListener('touchend', (event) => {
  const difference = event.changedTouches[0].screenX - touchStartX;
  if (Math.abs(difference) < 60) return;
  if (difference < 0) nextSlide();
  if (difference > 0) previousSlide();
}, { passive: true });

window.addEventListener('hashchange', () => showSlide(getInitialSlide(), false));

buildOverview();
showSlide(currentSlide, false);

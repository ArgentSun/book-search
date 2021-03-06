<script>
import ramjet from 'ramjet';

const components = {};
const getPosition = (node, addOffset = false) => {
  const rect = node.getBoundingClientRect();
  const computedStyle = window.getComputedStyle(node);
  const marginTop = parseInt(computedStyle.marginTop, 10);
  const marginLeft = parseInt(computedStyle.marginLeft, 10);
  const topOffset = marginTop
    + (addOffset ? 1 : 0) * (window.pageYOffset || document.documentElement.scrollTop);

  return {
    top: `${rect.top - topOffset}px`,
    left: `${rect.left - marginLeft}px`,
    width: `${rect.width}px`,
    height: `${rect.height}px`,
    borderRadius: computedStyle.borderRadius,
    position: 'absolute',
  };
};
export default {
  name: 'shared-element',
  props: {
    tag: {
      type: String,
      default: 'div',
    },
    id: {
      type: String,
      required: true,
    },
    duration: {
      type: Number,
      duration: 400,
    },
    easing: {
      type: Function,
      default: ramjet.linear,
    },
  },
  data() {
    return {
      animating: false,
      transformer: {},
    };
  },
  methods: {
    cache() {
      components[this.id] = {
        el: this.$slots.default,
        pos: getPosition(this.$el.firstChild),
      };
    },
    cloneAndAppend() {
      const { el, pos } = components[this.id];
      const clone = el[0].elm.cloneNode(true);
      clone.setAttribute('data-clone', this.id);
      Object.assign(clone.style, pos);
      document.body.appendChild(clone);
    },
    bustCache() {
      Object.keys(components).forEach((id) => {
        components[id] = false;
      });
    },
    animate(cb = () => {
    }) {
      const a = document.querySelector(`[data-clone="${this.id}"]`);
      const b = this.$el.firstChild;
      this.animating = true;
      a.classList.add('shared-element-transition');
      b.classList.add('shared-element-transition');
      this.transformer = ramjet.transform(a, b, {
        duration: this.duration,
        easing: this.easing,
        appendToBody: true,
        done: () => {
          cb(a, b);
          a.classList.remove('shared-element-transition');
          b.classList.remove('shared-element-transition');
          this.animating = false;
          this.$emit('animation-end');
        },
      });
      ramjet.hide(a, b);
    },
    handleMatch() {
      this.cloneAndAppend();
      const cb = (a, b) => {
        ramjet.show(b);
      };
      this.$nextTick(() => {
        this.animate(cb);
        const clone = document.querySelector(`[data-clone="${this.id}"]`);
        document.body.removeChild(clone);
        this.cache();
      });
    },
  },
  mounted() {
    const match = components[this.id];
    if (match) {
      this.handleMatch();
    } else {
      this.cache();
      this.$emit('animation-end');
    }
  },
  beforeDestroy() {
    if (this.animating && this.transformer) {
      this.transformer.teardown();
    }
  },
  render(h) {
    return h(this.tag, [this.$slots.default]);
  },
};
</script>

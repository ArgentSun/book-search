<template>
<Card class="book-list">
  <template slot="title">
  Books
  <template v-if="hasPermission('users.manage')">
  <span>&middot;</span>
  <router-link :to="{name: 'user-management'}">Users</router-link>
  <span>&middot;</span>
  <router-link :to="{name: 'role-management'}">Roles</router-link>
  </template>
  </template>
  <SeriesEntry :series="series" class="book-list-root" @book-deleted="refresh"></SeriesEntry>
  <template slot="footer">
  <a href="#" @click.prevent="$router.back()">Back</a>
  <div class="card-button-group">
    <Button slim @click="reindexAll" :loading="reindexing" :disabled="reindexing">
      Reindex all
    </Button>
    <Button slim :to="{name: 'book-upload'}" :disabled="reindexing">Upload</Button>
  </div>
  </template>
</Card>
</template>

<script>
import { mapGetters } from 'vuex';
import Card from '@/components/Card.vue';
import SeriesEntry from '@/components/management/SeriesEntry.vue';
import Button from '@/components/Button.vue';

export default {
  name: 'BookList',
  components: { Button, SeriesEntry, Card },
  data() {
    return {
      series: [],
      reindexing: false,
    };
  },
  computed: mapGetters('auth', ['hasPermission']),
  async mounted() {
    await this.refresh();
  },
  methods: {
    async refresh() {
      try {
        const { data: series } = await this.$api.get('/books/series');
        this.series = series;
      } catch (error) {
        this.$handleApiError(error);
      }
    },
    async reindexAll() {
      this.reindexing = true;
      try {
        const { data: { message } } = await this.$api.post('/books/reindex-all');
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      await this.refresh();
      this.reindexing = false;
    },
  },
};
</script>

<style lang="scss">
.book-list {
  margin: 0 auto;
  width: 50vw;
  max-width: $max-content-width;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  box-sizing: border-box;

  h2 {
    display: flex;
    align-items: center;

    span {
      margin-left: 0.25rem;
    }

    a {
      margin-left: 0.25rem;
    }
  }

  &-root {
    overflow-y: auto;
    flex-grow: 1;
    max-height: 100%;
  }

  @media (max-width: $max-content-width) {
    width: 100%;
  }
}
</style>

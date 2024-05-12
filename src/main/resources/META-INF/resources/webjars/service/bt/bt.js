define(function () {
	var current = {

		/**
		 * SLA table
		 */
		slasTable: null,

		/**
		 * Edited SLA object
		 */
		sla: null,

		/**
		 * Edited Business hours object
		 */
		businessHours: null,

		/**
		 * SLA objects
		 */
		slas: null,

		/**
		 * Edited Subscription
		 */
		model: null,

		/**
		 * Synchronize the SLA configuration UI with the retrieved data
		 */
		configure: function (configuration) {
			current.model = configuration;
			require(['jquery-ui'], function() {
				current.initializeSlaConfiguration();
				_('sla-calendar').select2('data', current.model.configuration.calendar);
				current.slasTable.fnClearTable();
				current.slasTable.fnAddData(current.model.configuration.slas);
				current.slasTable.fnDraw();
				const container = _('business-hours-content');
				container.empty();
				container.append($('<div class="progress-bar bar-info"></div>'));
				current.model.configuration.businessHours.forEach(current.addBusinessHours);
				_('subscribe-configuration-bt').removeClass('hide');
			});
		},

		/**
		 * Add a new business hours UI. Data is not updated. Return the created UI.
		 */
		addBusinessHours: function (businessHours) {
			const $bar = $('<div class="progress-bar progress-bar-danger" data-toggle="modal" data-target="#businessHoursPopup" data-id="' + businessHours.id + '"></div>');
			$bar.append($('<span class="start"></span>'));
			$bar.append($('<span class="end"></span>'));
			_('business-hours-content').append($bar);
			current.synchronizeBar($bar, businessHours, true);
			current.enableBusinessHoursDrag($bar);
			return $bar;
		},

		/**
		 * Update the bar to reflect the business hours values.
		 */
		synchronizeBar: function ($bar, businessHours, setX) {
			$bar.find('.start').text(momentManager.time(businessHours.start));
			$bar.find('.end').text(momentManager.time(businessHours.end));
			if (setX) {
				// Round the position regarding the resolution.
				// 7:12 -> will be visually displayed as 7:00
				// 7:40 -> will be visually displayed as 7:30
				const startForUi = Math.round(businessHours.start / current.BUSINESS_HOURS_RESOLUTION) * current.BUSINESS_HOURS_RESOLUTION;
				const left = current.milliToPercent(startForUi);
				const durationForUi = (Math.round(businessHours.end / current.BUSINESS_HOURS_RESOLUTION) * current.BUSINESS_HOURS_RESOLUTION) - startForUi;
				const width = current.milliToPercent(durationForUi);
				$bar.attr('style', 'width:' + width + '%;left:' + left + '%');
			}
		},

		/**
		 * Return the business hours configuration from its identifier.
		 */
		getBusinessHoursById: function (id) {
			return current.model.configuration.businessHours.find(bh => bh.id === id) || null;
		},
		/**
		 * Return the business hours configuration the UI element.
		 */
		getBusinessHoursFromUi: function ($bar) {
			return current.getBusinessHoursById(parseInt($bar.attr('data-id'), 10));
		},

		/**
		 * Update the grid option according to the grid width.
		 */
		onStartDrag: function (e) {
			// Resolution is 30min, so 48 fragments per day
			const $bar = $(this);
			const container = $bar.closest('.progress');
			const width = container.width();
			const gridResolution = width / 48;
			const businessHours = current.getBusinessHoursFromUi($bar);
			let boundBusinessHours = null;

			$bar.draggable('option', 'grid', [gridResolution, undefined]).resizable('option', 'grid', [gridResolution, undefined]);

			if ($(e.originalEvent.target).is('.ui-resizable-w')) {
				// WEST resize, left business hours (or 00:00) must we detected to fix a max width
				boundBusinessHours = current.getPreviousBusinessHours(businessHours.start);
				$bar.resizable('option', 'maxWidth', current.milliToPixel($bar, businessHours.end - boundBusinessHours.end) + 3);
			} else if ($(e.originalEvent.target).is('.ui-resizable-e')) {
				// EAST resize, right business hours (or 00:00) must we detected to fix a max width
				boundBusinessHours = current.getNextBusinessHours(businessHours.end);
				$bar.resizable('option', 'maxWidth', current.milliToPixel($bar, boundBusinessHours.start - businessHours.start) + 3);
			}
		},

		/**
		 * Return the first non business available hours from a starting millisecond.
		 */
		getFirstNonBusinessBusinessHours: function (time) {
			const businessHour = current.getBusinessHoursOfTime(time);
			time = businessHour ? businessHour.end : time;
			const closest = current.getNextBusinessHours(time);
			if (closest.start > time) {
				// There is a free time between time and next business hours
				return {start: time, end: closest.start};
			}
			if (closest.start >= 3600 * 24 * 1000) {
				return {
					start: 3600 * 24 * 1000
				};
			}
			if (closest.start === time) {
				// There is an immediate business hour, try the next one
				return current.getFirstNonBusinessBusinessHours(closest.end);
			}
			return null;
		},

		/**
		 * Closest business hours (or 00:00) just after the given time.
		 */
		getNextBusinessHours: function (time) {
			let boundBusinessHours = {
				start: 3600 * 24 * 1000
			};
			for (let businessHours of current.model.configuration.businessHours) {
				if (businessHours.start < boundBusinessHours.start && businessHours.start >= time) {
					boundBusinessHours = businessHours;
				}
			}
			return boundBusinessHours;
		},

		/**
		 * Closest business hours (or 00:00) just before the given time
		 */
		getPreviousBusinessHours: function (time) {
			let boundBusinessHours = {
				end: 0
			};
			for (let businessHours of current.model.configuration.businessHours) {
				if (businessHours.end > boundBusinessHours.end && businessHours.end <= time) {
					boundBusinessHours = businessHours;
				}
			}
			return boundBusinessHours;
		},

		/**
		 * Return the business hours containing the given time, or null.
		 */
		getBusinessHoursOfTime: function (time) {
		    return current.model.configuration.businessHours.find(bh => bh.start <= time && time <= bh.end) || null;
		},

		/**
		 * Enable UI assignment D&D feature.
		 */
		enableBusinessHoursDrag: function ($bar) {
			let businessHours = current.getBusinessHoursFromUi($bar);
			$bar.draggable({
				axis: 'x',
				addClasses: false,
				opacity: 0.6,
				cursor: 'move',
				drag: function (_i, data) {
					// live update the start/end dates
					let start0 = businessHours.start;
					let start1 = current.pixelToMilli($bar, data.position.left);
					let end1 = businessHours.end + start1 - start0;
					if (current.getPreviousBusinessHours(businessHours.start).end > start1 || current.getNextBusinessHours(businessHours.end).start < end1) {
						// Invalidate this move
						return false;
					}
					current.synchronizeBar($bar, {
						start: start1,
						end: end1
					}, false);
				},
				start: current.onStartDrag,
				stop: function (element, data) {
					let start0 = businessHours.start;
					let start1 = current.pixelToMilli($bar, data.position.left);
					let end1 = businessHours.end + start1 - start0;
					let fixedPosition = 0;

					// Fix the position.
					// Draggable as already commit the position even if the 'drag' event had invalidated
					if (current.getPreviousBusinessHours(businessHours.start).end > start1) {
						// Stick the start to the other bound and adjust the end
						fixedPosition = Math.ceil(current.getPreviousBusinessHours(businessHours.start).end / current.BUSINESS_HOURS_RESOLUTION) * current.BUSINESS_HOURS_RESOLUTION;
						end1 += fixedPosition - start1;
						start1 = fixedPosition;
					} else if (current.getNextBusinessHours(businessHours.end).start < end1) {
						// Stick the end to the other bound and adjust the start
						fixedPosition = Math.floor(current.getNextBusinessHours(businessHours.end).start / current.BUSINESS_HOURS_RESOLUTION) * current.BUSINESS_HOURS_RESOLUTION;
						start1 += fixedPosition - end1;
						end1 = fixedPosition;
					}

					// Update the UI
					if (start0 === start1) {
						// Restore original UI only
						current.synchronizeBar($bar, businessHours);
					} else {
						// Update business dates and then UI
						businessHours.start = start1;
						businessHours.end = end1;
						current.saveOrUpdateBusinessHours(businessHours);
					}
				}
			}).resizable({
				handles: 'e,w',
				resize: function (_i, data) {
					// live update the start/end dates
					const start = current.pixelToMilli($bar, data.position.left);
					const end = current.pixelToMilli($bar, data.position.left + data.size.width);
					if (current.getPreviousBusinessHours(businessHours.start).end > start || current.getNextBusinessHours(businessHours.end).start < end) {
						// Invalidate this resize
						return false;
					}
					current.synchronizeBar($bar, { start, end }, false);
				},
				start: current.onStartDrag,
				stop: function (element, data) {
					const start0 = businessHours.start;
					const end0 = businessHours.end;
					const start1 = current.pixelToMilli($bar, data.position.left);
					const end1 = current.pixelToMilli($bar, data.position.left + data.size.width);

					// Update the UI
					if (start0 === start1 && end0 === end1) {
						// Restore original UI
						current.synchronizeBar($bar, businessHours);
					} else {
						// Update business dates and then UI
						businessHours.start = start1;
						businessHours.end = end1;
						current.saveOrUpdateBusinessHours(businessHours);
					}
				}
			});
		},

		/**
		 * Grid resolution of business hours, in milliseconds.
		 */
		BUSINESS_HOURS_RESOLUTION: 60 / 2 * 60 * 1000,

		/**
		 * Convert a pixel value position to milliseconds.
		 */
		pixelToMilli: function ($bar, pixel) {
			// Resolution is 30min
			const gridResolution = $bar.draggable('option').grid[0];
			const shiftNb = Math.round(pixel / gridResolution);
			return current.BUSINESS_HOURS_RESOLUTION * shiftNb;
		},

		/**
		 * Convert a milliseconds value position to percent.
		 */
		milliToPercent: function (time) {
			return Math.round(time * 100 / (3600 * 24 * 1000) * 10) / 10;
		},

		/**
		 * Convert a milliseconds value position to pixel.
		 */
		milliToPixel: function ($bar, milliseconds) {
			// Resolution is 30min
			const width = $bar.closest('.progress').width();
			return Math.round(width * milliseconds / (3600 * 24 * 1000));
		},

		/**
		 * Build the SLAs table
		 */
		initializeSlasTable: function () {
			current.slasTable = _('slas').dataTable({
				dom: '<"row"<"col-xs-6"B>>t',
				pageLength: -1,
				columns: [
					{
						data: 'name',
						render: function (_i, _j, data) {
							return '<a data-id="' + data.id + '" data-toggle="modal" data-target="#slaPopup">' + data.name + '</a>';
						}
					}, {
						data: 'description',
						className: 'hidden-xs hidden-sm truncate'
					}, {
						data: 'start',
						className: 'truncate',
						render: function (_i, _j, data) {
							return data.start.join(', ');
						}
					}, {
						data: 'pause',
						className: 'hidden-xs truncate',
						render: function (_i, _j, data) {
							return data.pause.join(', ');
						}
					}, {
						data: 'stop',
						className: 'truncate',
						render: function (_i, _j, data) {
							return data.stop.join(', ');
						}
					}, {
						data: 'priorities',
						className: 'hidden-xs hidden-sm truncate',
						render: function (_i, _j, data) {
							return data.priorities.join(', ');
						}
					}, {
						data: 'resolutions',
						className: 'hidden-xs hidden-sm truncate',
						render: function (_i, _j, data) {
							return data.resolutions.join(', ');
						}
					}, {
						data: 'types',
						className: 'hidden-xs hidden-sm truncate',
						render: function (_i, _j, data) {
							return data.types.join(', ');
						}
					}, {
						data: 'threshold',
						className: 'truncate',
						width: '50px',
						render: function (_i, _j, data) {
							if (data.threshold > 0) {
								return momentManager.duration(data.threshold);
							}
							return '&nbsp;';
						}
					}, {
						data: null,
						width: '16px',
						orderable: false,
						render: function () {
							// Delete link
							return '<a class="delete"><i class="fas fa-times" data-toggle="tooltip" title="' + current.$messages['delete'] + '"></i></a>';
						}
					}
				],
				createdRow: function (nRow) {
					$(nRow).find('.delete').on('click', current.deleteSla);
				},
				buttons: [
					{
						extend: 'popup',
						className: 'btn-success btn-raised',
						text: current.$messages.add,
						target: '#slaPopup'
					}
				],
				destroy: true,
				data: []
			});
		},

		/**
		 * Delete the selected SLA after popup confirmation, or directly from its identifier.
		 */
		deleteSla: function (id, name) {
			if ((typeof id) === 'number') {
				// Delete without confirmation
				$.ajax({
					type: 'DELETE',
					url: REST_PATH + 'service/bt/sla/' + id,
					success: function () {
						// Refresh the table without additional query
						const tr = _('slas').find('a[data-id="' + id + '"]').closest('tr')[0];
						current.slasTable.fnDeleteRow(tr);
						notifyManager.notify(Handlebars.compile(current.$messages.deleted)(name));
					}
				});
			} else {
				// Requires a confirmation
				const entity = current.slasTable.fnGetData($(this).closest('tr')[0]);
				bootbox.confirmDelete(function (confirmed) {
					confirmed && current.deleteSla(entity.id, entity.name);
				}, entity.name);
			}
		},

		/**
		 * Delete the given business hours.
		 */
		deleteBusinessHours: function (id) {
			// Delete without confirmation
			$.ajax({
				type: 'DELETE',
				url: REST_PATH + 'service/bt/business-hours/' + id,
				success: function () {
					// Update data of the local store
					current.businessHours = current.getBusinessHoursById(id);
					for (let index = 0; index < current.model.configuration.businessHours.length; index++) {
						if (current.model.configuration.businessHours[index].id === id) {
							current.model.configuration.businessHours.splice(index, 1);
							break;
						}
					}

					// Update the UI
					_('business-hours-content').find('[data-id="' + id + '"]').remove();
					_('businessHoursPopup').modal('hide');
					notifyManager.notify((Handlebars.compile(current.$messages.deleted))(id));
				}
			});
		},

		/**
		 * Return the SLA configuration from its identifier. The SLA dataTable is used to retrieve the SLA.
		 */
		getSlaById: function (id) {
			return current.slasTable.fnGetData(_('slas').find('a[data-id="' + id + '"]').closest('tr')[0]);
		},

		/**
		 * Save or create given SLA. If not provided, the object will be built from the Popup values.
		 * @param {Object} sla the SLA to create/update
		 */
		saveOrUpdateSla: function (sla) {
			if ((typeof sla) === 'undefined' || sla === null) {
				const hours = parseInt(_('sla-threshold-hour').val(), 10);
				const minutes = parseInt(_('sla-threshold-minute').val(), 10);
				const seconds = parseInt(_('sla-threshold-second').val(), 10);
				sla = {
					id: (current.sla && current.sla.id) || undefined,
					name: _('sla-name').val(),
					description: _('sla-description').val(),
					start: _('sla-start').val().split(','),
					stop: _('sla-stop').val().split(','),
					pause: _('sla-pause').val().split(','),
					types: _('sla-type').val().split(','),
					priorities: _('sla-priority').val().split(','),
					resolutions: _('sla-resolution').val().split(','),
					threshold: ((hours * 3600) + (minutes * 60) + seconds) * 1000
				};
			}
			sla.subscription = current.model.subscription;

			// Save SLA on server
			$.ajax({
				type: sla.id ? 'PUT' : 'POST',
				url: REST_PATH + 'service/bt/sla',
				dataType: 'json',
				contentType: 'application/json',
				data: JSON.stringify(sla),
				success: function (data) {
					_('slaPopup').modal('hide');

					// Refresh the table without additional query
					if (sla.id) {
						// Update data of the local store
						current.sla = $.extend(current.getSlaById(sla.id), sla);

						// Update the UI
						const tr = _('slas').find('a[data-id="' + sla.id + '"]').closest('tr')[0];
						notifyManager.notify((Handlebars.compile(current.$messages.updated))(sla.name));
						current.slasTable.fnUpdate(sla, tr);
					} else {
						// Update the new object and add it to the local store
						sla.id = data;
						current.sla = sla;
						current.slasTable.fnAddData(sla, true);
						notifyManager.notify((Handlebars.compile(current.$messages.created))(sla.name));
					}
				}
			});
		},
		/**
		 * Save or create given business hours. If not provided, the object will be built from the Popup
		 * values.
		 * @param {Object} businessHours the business hours to create/update
		 */
		saveOrUpdateBusinessHours: function (businessHours) {
			if ((typeof businessHours) === 'undefined' || businessHours === null) {
				businessHours = {
					id: (current.businessHours && current.businessHours.id) || undefined,
					start: $('#sla-business-hours-start').timepickerTime(),
					end: $('#sla-business-hours-end').timepickerTime()
				};
			}
			businessHours.subscription = current.model.subscription;

			// Save business hours on server
			$.ajax({
				type: businessHours.id ? 'PUT' : 'POST',
				url: REST_PATH + 'service/bt/business-hours',
				dataType: 'json',
				contentType: 'application/json',
				data: JSON.stringify(businessHours),
				success: function (data) {
					_('businessHoursPopup').modal('hide');

					// Refresh the table without additional query
					if (businessHours.id) {
						// Update data of the local store
						current.businessHours = $.extend(current.getBusinessHoursById(businessHours.id), businessHours);

						// Update the UI
						const $bar = _('business-hours-content').find('[data-id="' + businessHours.id + '"]');
						current.synchronizeBar($bar, businessHours, true);
						notifyManager.notify((Handlebars.compile(current.$messages.updated))(businessHours.id));
					} else {
						// Update the new object and add it to the local store
						businessHours.id = data;
						current.businessHours = businessHours;
						current.model.configuration.businessHours.push(businessHours);

						// Update the UI
						current.addBusinessHours(businessHours).fadeIn(1500);
						notifyManager.notify((Handlebars.compile(current.$messages.created))(businessHours.id));
					}
				}
			});
		},

		/**
		 * Initialize SLA configuration UI components
		 */
		initializeSlaConfiguration: function () {
			current.initializeSlasTable();

			// Calendar association management
			_('sla-calendar').select2({
				formatResult: current.$super('toName'),
				formatSelection: current.$super('toName'),
				ajax: {
					url: REST_PATH + 'service/bt/calendar',
					dataType: 'json',
					results: function (data) {
						return {results: data};
					}
				}
			}).on('change', function (e) {
				// Save the new calendar association
				$.ajax({
					type: 'PUT',
					url: REST_PATH + 'service/bt/calendar/' + current.model.subscription + '/' + e.val
				});
			});

			// SLA Pop-up management
			_('slaPopup').on('shown.bs.modal', function () {
				_('sla-name').focus();
			}).on('show.bs.modal', function (event) {
				validationManager.reset($(this));
				validationManager.mapping.name = 'sla-name';
				validationManager.mapping.description = 'sla-description';
				validationManager.mapping.start = 'sla-start';
				validationManager.mapping.stop = 'sla-stop';
				validationManager.mapping.pause = 'sla-pause';
				validationManager.mapping.types = 'sla-type';
				validationManager.mapping.priorities = 'sla-priority';
				validationManager.mapping.resolutions = 'sla-resolution';
				validationManager.mapping.threshold = 'sla-threshold-hour';
				const $source = $(event.relatedTarget);
				let uc = $source && current.slasTable.fnGetData($source.closest('tr')[0]);
				const duration = moment.duration((uc && uc.id && uc.threshold) || 0);
				const select2Configuration = {
					multiple: true,
					createSearchChoice: function () {
						// Disable additional values
						return null;
					},
					formatResult: current.$super('toText'),
					formatSelection: current.$super('toText')
				};
				const statusConfiguration = $.extend({
					tags: current.model.configuration.statuses
				}, select2Configuration);

				current.sla = (uc && uc.id) ? uc : null;
				uc = current.sla || {};
				_('sla-name').val(uc.name || '');
				_('sla-description').val(uc.description || '');

				// Initialize the 3 select2 tags of statuses
				_('sla-start').select2(statusConfiguration).select2('val', uc.start || []);
				_('sla-pause').select2(statusConfiguration).select2('val', uc.pause || []);
				_('sla-stop').select2(statusConfiguration).select2('val', uc.stop || []);
				_('sla-type').select2($.extend({
					tags: current.model.configuration.types
				}, select2Configuration)).select2('val', uc.types || []);
				_('sla-priority').select2($.extend({
					tags: current.model.configuration.priorities
				}, select2Configuration)).select2('val', uc.priorities || []);
				_('sla-resolution').select2($.extend({
					tags: current.model.configuration.resolutions
				}, select2Configuration)).select2('val', uc.resolutions || []);

				// Split the threshold in 3 spinner units
				_('sla-threshold-minute').spinner('value', duration.minutes());
				_('sla-threshold-second').spinner('value', duration.seconds());
				_('sla-threshold-hour').spinner('value', Math.floor(duration.asHours()));
			}).on('submit', function (e) {
				e.preventDefault();
				current.saveOrUpdateSla();
				return false;
			});
			_('sla-threshold-hour').spinner({step: 1, min: 0});
			_('sla-threshold-minute').spinner({step: 15, max: 59, min: 0});
			_('sla-threshold-second').spinner({step: 15, max: 59, min: 0});

			// Business hours pop-up management
			_('businessHoursPopup').on('shown.bs.modal', function () {
				_('sla-business-hours-start').focus();
			}).on('show.bs.modal', function (event) {
				validationManager.reset(_('businessHoursPopup'));
				validationManager.mapping.start = 'sla-business-hours-start';
				validationManager.mapping.end = 'sla-business-hours-end';
				const $source = $(event.relatedTarget);
				let uc = $source && current.getBusinessHoursFromUi($source);
				const firstNonBusinessHours;

				current.businessHours = (uc && uc.id) ? uc : null;
				uc = current.businessHours || {};
				if (current.businessHours) {
					// Edition mode, initialize the times with current business hours
					$(this).find('input[type="submit"]').removeClass('btn-success').addClass('btn-primary');

					// Delete button is display only if it's not the last business hours
					if (current.model.configuration.businessHours.length === 1) {
						_('business-hours-delete').addClass('hide');
					} else {
						_('business-hours-delete').removeClass('hide');
					}

					_('sla-business-hours-start').timepicker('setTime', momentManager.time(uc.start));
					_('sla-business-hours-end').timepicker('setTime', momentManager.time(uc.end));
				} else {
					// Creation mode, initialize the times with valid free times
					$(this).find('input[type="submit"]').removeClass('btn-primary').addClass('btn-success');
					firstNonBusinessHours = current.getFirstNonBusinessBusinessHours(0);
					_('business-hours-delete').addClass('hide');
					_('sla-business-hours-start').timepicker('setTime', momentManager.time(firstNonBusinessHours.start));
					_('sla-business-hours-end').timepicker('setTime', momentManager.time(firstNonBusinessHours.end));
				}
			}).on('submit', function (e) {
				e.preventDefault();
				current.saveOrUpdateBusinessHours();
				return false;
			});

			_('business-hours-delete').on('click', function () {
				// Deletion the current business hours
				current.deleteBusinessHours(current.businessHours.id);
				return false;
			});
		},

		/**
		 * Render Bug tracking data.
		 */
		renderFeatures: function (subscription) {
			return current.$super('renderServiceLink')('clock', '#/home/project/' + subscription.project + '/subscription/' + subscription.id, 'service:bt:sla');
		}
	};
	return current;
});

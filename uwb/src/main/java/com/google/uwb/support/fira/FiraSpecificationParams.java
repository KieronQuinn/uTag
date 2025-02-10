/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.uwb.support.fira;

import static java.util.Objects.requireNonNull;

import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import com.google.uwb.support.base.FlagEnum;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Defines parameters for FIRA capability.
 *
 * <p>This is returned as a bundle from the service API {@link UwbManager#getSpecificationInfo}.
 */
public class FiraSpecificationParams extends FiraParams {
    private static final int BUNDLE_VERSION_1 = 1;
    private static final int BUNDLE_VERSION_2 = 2;
    private static final int BUNDLE_VERSION_CURRENT = BUNDLE_VERSION_2;

    private final FiraProtocolVersion mMinPhyVersionSupported;
    private final FiraProtocolVersion mMaxPhyVersionSupported;
    private final FiraProtocolVersion mMinMacVersionSupported;
    private final FiraProtocolVersion mMaxMacVersionSupported;

    private final List<Integer> mSupportedChannels;

    private final EnumSet<AoaCapabilityFlag> mAoaCapabilities;

    private final EnumSet<DeviceRoleCapabilityFlag> mDeviceRoleCapabilities;

    private final boolean mHasBlockStridingSupport;

    private final boolean mHasHoppingPreferenceSupport;

    private final boolean mHasExtendedMacAddressSupport;

    private final boolean mHasNonDeferredModeSupport;

    private final boolean mHasInitiationTimeSupport;

    private final boolean mHasRssiReportingSupport;

    private final boolean mHasDiagnosticsSupport;

    private final int mMinRangingInterval;

    private final int mMinSlotDurationUs;

    private final int mMaxRangingSessionNumber;

    private final EnumSet<MultiNodeCapabilityFlag> mMultiNodeCapabilities;

    private final EnumSet<RangingTimeStructCapabilitiesFlag> mRangingTimeStructCapabilities;

    private final EnumSet<SchedulingModeCapabilitiesFlag> mSchedulingModeCapabilities;

    private final EnumSet<CcConstraintLengthCapabilitiesFlag> mCcConstraintLengthCapabilities;

    private final EnumSet<PrfCapabilityFlag> mPrfCapabilities;

    private final EnumSet<RangingRoundCapabilityFlag> mRangingRoundCapabilities;

    private final EnumSet<RframeCapabilityFlag> mRframeCapabilities;

    private final EnumSet<StsCapabilityFlag> mStsCapabilities;

    private final EnumSet<PsduDataRateCapabilityFlag> mPsduDataRateCapabilities;

    private final EnumSet<BprfParameterSetCapabilityFlag> mBprfParameterSetCapabilities;

    private final EnumSet<HprfParameterSetCapabilityFlag> mHprfParameterSetCapabilities;

    private final Integer mMaxMessageSize;

    private final Integer mMaxDataPacketPayloadSize;

    private final EnumSet<RangeDataNtfConfigCapabilityFlag> mRangeDataNtfConfigCapabilities;

    private final int mDeviceType;

    private final boolean mSuspendRangingSupport;

    private final int mSessionKeyLength;

    private final int mDtTagMaxActiveRr;

    private final boolean mHasBackgroundRangingSupport;

    private final boolean mHasDtTagBlockSkippingSupport;

    private final boolean mHasPsduLengthSupport;

    private final int mUciVersion;

    private static final String KEY_MIN_PHY_VERSION = "min_phy_version";
    private static final String KEY_MAX_PHY_VERSION = "max_phy_version";
    private static final String KEY_MIN_MAC_VERSION = "min_mac_version";
    private static final String KEY_MAX_MAC_VERSION = "max_mac_version";

    private static final String KEY_SUPPORTED_CHANNELS = "channels";
    private static final String KEY_AOA_CAPABILITIES = "aoa_capabilities";
    private static final String KEY_DEVICE_ROLE_CAPABILITIES = "device_role_capabilities";
    private static final String KEY_BLOCK_STRIDING_SUPPORT = "block_striding";
    private static final String KEY_HOPPING_PREFERENCE_SUPPORT = "hopping_preference";
    private static final String KEY_EXTENDED_MAC_ADDRESS_SUPPORT = "extended_mac_address";
    private static final String KEY_NON_DEFERRED_MODE_SUPPORT = "non_deferred_mode";
    private static final String KEY_INITIATION_TIME_SUPPORT = "initiation_time";
    private static final String KEY_RSSI_REPORTING_SUPPORT = "rssi_reporting";
    private static final String KEY_DIAGNOSTICS_SUPPORT = "diagnostics";
    private static final String KEY_MIN_RANGING_INTERVAL = "min_ranging_interval";
    private static final String KEY_MIN_SLOT_DURATION = "min_slot_duration";
    private static final String KEY_MAX_RANGING_SESSION_NUMBER = "max_ranging_session_number";
    private static final String KEY_MULTI_NODE_CAPABILITIES = "multi_node_capabilities";
    private static final String KEY_RANGING_TIME_STRUCT_CAPABILITIES =
            "ranging_time_struct_capabilities";
    private static final String KEY_SCHEDULING_MODE_CAPABILITIES = "scheduling_mode_capabilities";
    private static final String KEY_CC_CONSTRAINT_LENGTH_CAPABILITIES =
            "cc_constraint_length_capabilities";
    private static final String KEY_PRF_CAPABILITIES = "prf_capabilities";
    private static final String KEY_RANGING_ROUND_CAPABILITIES =
            "ranging_round_capabilities";
    private static final String KEY_RFRAME_CAPABILITIES = "rframe_capabilities";
    private static final String KEY_STS_CAPABILITIES = "sts_capabilities";
    private static final String KEY_PSDU_DATA_RATE_CAPABILITIES = "psdu_data_rate_capabilities";
    private static final String KEY_BPRF_PARAMETER_SET_CAPABILITIES =
            "bprf_parameter_set_capabilities";
    private static final String KEY_HPRF_PARAMETER_SET_CAPABILITIES =
            "hprf_parameter_set_capabilities";
    private static final String KEY_MAX_MESSAGE_SIZE = "max_message_size";
    private static final String KEY_MAX_DATA_PACKET_PAYLOAD_SIZE = "max_data_packet_payload_size";
    private static final String KEY_RANGE_DATA_NTF_CONFIG_CAPABILITIES =
            "range_data_ntf_config_capabilities";
    private static final String KEY_DEVICE_TYPE =
            "device_type";
    private static final String KEY_SUSPEND_RANGING_SUPPORT =
            "suspend_ranging_support";
    private static final String KEY_SESSION_KEY_LENGTH =
            "session_key_length";
    public static final String DT_TAG_MAX_ACTIVE_RR = "dt_tag_max_active_rr";

    public static final String KEY_BACKGROUND_RANGING_SUPPORT = "background_ranging_support";

    public static final String KEY_DT_TAG_BLOCK_SKIPPING_SUPPORT = "dt_tag_block_skipping";

    public static final String KEY_PSDU_LENGTH_SUPPORT = "psdu_length_support";

    public static final String KEY_UCI_VERSION = "uci_version";

    public static final int DEFAULT_MAX_RANGING_SESSIONS_NUMBER = 5;

    private FiraSpecificationParams(
            FiraProtocolVersion minPhyVersionSupported,
            FiraProtocolVersion maxPhyVersionSupported,
            FiraProtocolVersion minMacVersionSupported,
            FiraProtocolVersion maxMacVersionSupported,
            List<Integer> supportedChannels,
            EnumSet<AoaCapabilityFlag> aoaCapabilities,
            EnumSet<DeviceRoleCapabilityFlag> deviceRoleCapabilities,
            boolean hasBlockStridingSupport,
            boolean hasHoppingPreferenceSupport,
            boolean hasExtendedMacAddressSupport,
            boolean hasNonDeferredModeSupport,
            boolean hasInitiationTimeSupport,
            boolean hasRssiReportingSupport,
            boolean hasDiagnosticsSupport,
            int minRangingInterval,
            int minSlotDurationUs,
            int maxRangingSessionNumber,
            EnumSet<MultiNodeCapabilityFlag> multiNodeCapabilities,
            EnumSet<RangingTimeStructCapabilitiesFlag> rangingTimeStructCapabilities,
            EnumSet<SchedulingModeCapabilitiesFlag> schedulingModeCapabilities,
            EnumSet<CcConstraintLengthCapabilitiesFlag> ccConstraintLengthCapabilities,
            EnumSet<PrfCapabilityFlag> prfCapabilities,
            EnumSet<RangingRoundCapabilityFlag> rangingRoundCapabilities,
            EnumSet<RframeCapabilityFlag> rframeCapabilities,
            EnumSet<StsCapabilityFlag> stsCapabilities,
            EnumSet<PsduDataRateCapabilityFlag> psduDataRateCapabilities,
            EnumSet<BprfParameterSetCapabilityFlag> bprfParameterSetCapabilities,
            EnumSet<HprfParameterSetCapabilityFlag> hprfParameterSetCapabilities,
            Integer maxMessageSize,
            Integer maxDataPacketPayloadSize,
            EnumSet<RangeDataNtfConfigCapabilityFlag> rangeDataNtfConfigCapabilities,
            int deviceType, boolean suspendRangingSupport, int sessionKeyLength,
            int dtTagMaxActiveRr, boolean hasBackgroundRangingSupport,
            boolean hasDtTagBlockSkippingSupport, boolean hasPsduLengthSupport,
            int uciVersion) {
        mMinPhyVersionSupported = minPhyVersionSupported;
        mMaxPhyVersionSupported = maxPhyVersionSupported;
        mMinMacVersionSupported = minMacVersionSupported;
        mMaxMacVersionSupported = maxMacVersionSupported;
        mSupportedChannels = supportedChannels;
        mAoaCapabilities = aoaCapabilities;
        mDeviceRoleCapabilities = deviceRoleCapabilities;
        mHasBlockStridingSupport = hasBlockStridingSupport;
        mHasHoppingPreferenceSupport = hasHoppingPreferenceSupport;
        mHasExtendedMacAddressSupport = hasExtendedMacAddressSupport;
        mHasNonDeferredModeSupport = hasNonDeferredModeSupport;
        mHasInitiationTimeSupport = hasInitiationTimeSupport;
        mHasRssiReportingSupport = hasRssiReportingSupport;
        mHasDiagnosticsSupport = hasDiagnosticsSupport;
        mMinRangingInterval = minRangingInterval;
        mMinSlotDurationUs = minSlotDurationUs;
        mMaxRangingSessionNumber = maxRangingSessionNumber;
        mMultiNodeCapabilities = multiNodeCapabilities;
        mRangingTimeStructCapabilities = rangingTimeStructCapabilities;
        mSchedulingModeCapabilities = schedulingModeCapabilities;
        mCcConstraintLengthCapabilities = ccConstraintLengthCapabilities;
        mPrfCapabilities = prfCapabilities;
        mRangingRoundCapabilities = rangingRoundCapabilities;
        mRframeCapabilities = rframeCapabilities;
        mStsCapabilities = stsCapabilities;
        mPsduDataRateCapabilities = psduDataRateCapabilities;
        mBprfParameterSetCapabilities = bprfParameterSetCapabilities;
        mHprfParameterSetCapabilities = hprfParameterSetCapabilities;
        mMaxMessageSize = maxMessageSize;
        mMaxDataPacketPayloadSize = maxDataPacketPayloadSize;
        mRangeDataNtfConfigCapabilities = rangeDataNtfConfigCapabilities;
        mDeviceType = deviceType;
        mSuspendRangingSupport = suspendRangingSupport;
        mSessionKeyLength = sessionKeyLength;
        mDtTagMaxActiveRr = dtTagMaxActiveRr;
        mHasBackgroundRangingSupport = hasBackgroundRangingSupport;
        mHasDtTagBlockSkippingSupport = hasDtTagBlockSkippingSupport;
        mHasPsduLengthSupport = hasPsduLengthSupport;
        mUciVersion = uciVersion;
    }

    @Override
    protected int getBundleVersion() {
        return BUNDLE_VERSION_CURRENT;
    }

    public FiraProtocolVersion getMinPhyVersionSupported() {
        return mMinPhyVersionSupported;
    }

    public FiraProtocolVersion getMaxPhyVersionSupported() {
        return mMaxPhyVersionSupported;
    }

    public FiraProtocolVersion getMinMacVersionSupported() {
        return mMinMacVersionSupported;
    }

    public FiraProtocolVersion getMaxMacVersionSupported() {
        return mMaxMacVersionSupported;
    }

    public List<Integer> getSupportedChannels() {
        return mSupportedChannels;
    }

    public EnumSet<AoaCapabilityFlag> getAoaCapabilities() {
        return mAoaCapabilities;
    }

    public EnumSet<DeviceRoleCapabilityFlag> getDeviceRoleCapabilities() {
        return mDeviceRoleCapabilities;
    }

    public boolean hasBlockStridingSupport() {
        return mHasBlockStridingSupport;
    }

    public boolean hasHoppingPreferenceSupport() {
        return mHasHoppingPreferenceSupport;
    }

    public boolean hasExtendedMacAddressSupport() {
        return mHasExtendedMacAddressSupport;
    }

    public boolean hasNonDeferredModeSupport() {
        return mHasNonDeferredModeSupport;
    }

    public boolean hasInitiationTimeSupport() {
        return mHasInitiationTimeSupport;
    }

    /** get a boolean whether rssi reporting is supported. */
    public boolean hasRssiReportingSupport() {
        return mHasRssiReportingSupport;
    }

    /** get a boolean whether diagnostics is supported. */
    public boolean hasDiagnosticsSupport() {
        return mHasDiagnosticsSupport;
    }

    public int getMinRangingInterval() {
        return mMinRangingInterval;
    }

    public int getMinSlotDurationUs() {
        return mMinSlotDurationUs;
    }

    public int getMaxRangingSessionNumber() {
        return mMaxRangingSessionNumber;
    }

    public EnumSet<MultiNodeCapabilityFlag> getMultiNodeCapabilities() {
        return mMultiNodeCapabilities;
    }

    public EnumSet<RangingTimeStructCapabilitiesFlag> getRangingTimeStructCapabilities() {
        return mRangingTimeStructCapabilities;
    }

    public EnumSet<SchedulingModeCapabilitiesFlag> getSchedulingModeCapabilities() {
        return mSchedulingModeCapabilities;
    }

    public EnumSet<CcConstraintLengthCapabilitiesFlag> getCcConstraintLengthCapabilities() {
        return mCcConstraintLengthCapabilities;
    }

    public EnumSet<PrfCapabilityFlag> getPrfCapabilities() {
        return mPrfCapabilities;
    }

    public EnumSet<RangingRoundCapabilityFlag> getRangingRoundCapabilities() {
        return mRangingRoundCapabilities;
    }

    public EnumSet<RframeCapabilityFlag> getRframeCapabilities() {
        return mRframeCapabilities;
    }

    public EnumSet<StsCapabilityFlag> getStsCapabilities() {
        return mStsCapabilities;
    }

    public EnumSet<PsduDataRateCapabilityFlag> getPsduDataRateCapabilities() {
        return mPsduDataRateCapabilities;
    }

    public EnumSet<BprfParameterSetCapabilityFlag> getBprfParameterSetCapabilities() {
        return mBprfParameterSetCapabilities;
    }

    public EnumSet<HprfParameterSetCapabilityFlag> getHprfParameterSetCapabilities() {
        return mHprfParameterSetCapabilities;
    }

    public EnumSet<RangeDataNtfConfigCapabilityFlag> getRangeDataNtfConfigCapabilities() {
        return mRangeDataNtfConfigCapabilities;
    }

    public Integer getMaxMessageSize() {
        return mMaxMessageSize;
    }

    public Integer getMaxDataPacketPayloadSize() {
        return mMaxDataPacketPayloadSize;
    }

    public int getDeviceType() {
        return mDeviceType;
    }

    public boolean hasSuspendRangingSupport() {
        return mSuspendRangingSupport;
    }

    public int getSessionKeyLength() {
        return mSessionKeyLength;
    }

    public int getDtTagMaxActiveRr() {
        return mDtTagMaxActiveRr;
    }

    public boolean hasBackgroundRangingSupport() {
        return mHasBackgroundRangingSupport;
    }

    public boolean hasDtTagBlockSkippingSupport() {
        return mHasDtTagBlockSkippingSupport;
    }

    public boolean hasPsduLengthSupport() {
        return mHasPsduLengthSupport;
    }

    public int getUciVersionSupported() {
        return mUciVersion;
    }

    private static int[] toIntArray(List<Integer> data) {
        int[] res = new int[data.size()];
        for (int i = 0; i < data.size(); i++) {
            res[i] = data.get(i);
        }
        return res;
    }

    @Override
    public PersistableBundle toBundle() {
        PersistableBundle bundle = super.toBundle();
        bundle.putString(KEY_MIN_PHY_VERSION, mMinPhyVersionSupported.toString());
        bundle.putString(KEY_MAX_PHY_VERSION, mMaxPhyVersionSupported.toString());
        bundle.putString(KEY_MIN_MAC_VERSION, mMinMacVersionSupported.toString());
        bundle.putString(KEY_MAX_MAC_VERSION, mMaxMacVersionSupported.toString());
        bundle.putIntArray(KEY_SUPPORTED_CHANNELS, toIntArray(mSupportedChannels));
        bundle.putInt(KEY_AOA_CAPABILITIES, FlagEnum.toInt(mAoaCapabilities));
        bundle.putInt(KEY_DEVICE_ROLE_CAPABILITIES, FlagEnum.toInt(mDeviceRoleCapabilities));
        bundle.putBoolean(KEY_BLOCK_STRIDING_SUPPORT, mHasBlockStridingSupport);
        bundle.putBoolean(KEY_HOPPING_PREFERENCE_SUPPORT, mHasHoppingPreferenceSupport);
        bundle.putBoolean(KEY_EXTENDED_MAC_ADDRESS_SUPPORT, mHasExtendedMacAddressSupport);
        bundle.putBoolean(KEY_NON_DEFERRED_MODE_SUPPORT, mHasNonDeferredModeSupport);
        bundle.putBoolean(KEY_INITIATION_TIME_SUPPORT, mHasInitiationTimeSupport);
        bundle.putBoolean(KEY_RSSI_REPORTING_SUPPORT, mHasRssiReportingSupport);
        bundle.putBoolean(KEY_DIAGNOSTICS_SUPPORT, mHasDiagnosticsSupport);
        bundle.putInt(KEY_MIN_RANGING_INTERVAL, mMinRangingInterval);
        bundle.putInt(KEY_MIN_SLOT_DURATION, mMinSlotDurationUs);
        bundle.putInt(KEY_MAX_RANGING_SESSION_NUMBER, mMaxRangingSessionNumber);
        bundle.putInt(KEY_MULTI_NODE_CAPABILITIES, FlagEnum.toInt(mMultiNodeCapabilities));
        bundle.putInt(KEY_RANGING_TIME_STRUCT_CAPABILITIES,
                FlagEnum.toInt(mRangingTimeStructCapabilities));
        bundle.putInt(KEY_SCHEDULING_MODE_CAPABILITIES,
                FlagEnum.toInt(mSchedulingModeCapabilities));
        bundle.putInt(KEY_CC_CONSTRAINT_LENGTH_CAPABILITIES,
                FlagEnum.toInt(mCcConstraintLengthCapabilities));
        bundle.putInt(KEY_PRF_CAPABILITIES, FlagEnum.toInt(mPrfCapabilities));
        bundle.putInt(KEY_RANGING_ROUND_CAPABILITIES, FlagEnum.toInt(mRangingRoundCapabilities));
        bundle.putInt(KEY_RFRAME_CAPABILITIES, FlagEnum.toInt(mRframeCapabilities));
        bundle.putInt(KEY_STS_CAPABILITIES, FlagEnum.toInt(mStsCapabilities));
        bundle.putInt(KEY_PSDU_DATA_RATE_CAPABILITIES, FlagEnum.toInt(mPsduDataRateCapabilities));
        bundle.putInt(KEY_BPRF_PARAMETER_SET_CAPABILITIES,
                FlagEnum.toInt(mBprfParameterSetCapabilities));
        bundle.putLong(KEY_HPRF_PARAMETER_SET_CAPABILITIES,
                FlagEnum.toLong(mHprfParameterSetCapabilities));
        bundle.putInt(KEY_MAX_MESSAGE_SIZE, mMaxMessageSize);
        bundle.putInt(KEY_MAX_DATA_PACKET_PAYLOAD_SIZE, mMaxDataPacketPayloadSize);
        bundle.putInt(KEY_RANGE_DATA_NTF_CONFIG_CAPABILITIES,
                FlagEnum.toInt(mRangeDataNtfConfigCapabilities));
        bundle.putInt(KEY_DEVICE_TYPE, mDeviceType);
        bundle.putBoolean(KEY_SUSPEND_RANGING_SUPPORT, mSuspendRangingSupport);
        bundle.putInt(KEY_SESSION_KEY_LENGTH, mSessionKeyLength);
        bundle.putInt(DT_TAG_MAX_ACTIVE_RR, mDtTagMaxActiveRr);
        bundle.putBoolean(KEY_BACKGROUND_RANGING_SUPPORT, mHasBackgroundRangingSupport);
        bundle.putBoolean(KEY_DT_TAG_BLOCK_SKIPPING_SUPPORT, mHasDtTagBlockSkippingSupport);
        bundle.putBoolean(KEY_PSDU_LENGTH_SUPPORT, mHasPsduLengthSupport);
        bundle.putInt(KEY_UCI_VERSION, mUciVersion);
        return bundle;
    }

    public static FiraSpecificationParams fromBundle(PersistableBundle bundle) {
        if (!isCorrectProtocol(bundle)) {
            throw new IllegalArgumentException("Invalid protocol");
        }

        switch (getBundleVersion(bundle)) {
            case BUNDLE_VERSION_1:
                return parseVersion1(bundle).build();
            case BUNDLE_VERSION_2:
                return parseVersion2(bundle).build();
            default:
                throw new IllegalArgumentException("Invalid bundle version");
        }
    }

    private static List<Integer> toIntList(int[] data) {
        List<Integer> res = new ArrayList<>();
        for (int datum : data) {
            res.add(datum);
        }
        return res;
    }

    private static Builder parseVersion2(PersistableBundle bundle) {
        Builder builder = parseVersion1(bundle);
        builder.setDeviceType(bundle.getInt(KEY_DEVICE_TYPE));
        builder.setSuspendRangingSupport(bundle.getBoolean(KEY_SUSPEND_RANGING_SUPPORT));
        builder.setSessionKeyLength(bundle.getInt(KEY_SESSION_KEY_LENGTH));
        builder.setDtTagMaxActiveRr(bundle.getInt(DT_TAG_MAX_ACTIVE_RR, 0));
        builder.setBackgroundRangingSupport(bundle.getBoolean(KEY_BACKGROUND_RANGING_SUPPORT));
        builder.setDtTagBlockSkippingSupport(bundle.getBoolean(KEY_DT_TAG_BLOCK_SKIPPING_SUPPORT));
        builder.setPsduLengthSupport(bundle.getBoolean(KEY_PSDU_LENGTH_SUPPORT));
        builder.setUciVersionSupported(bundle.getInt(KEY_UCI_VERSION, 1));
        return builder;
    }

    private static Builder parseVersion1(PersistableBundle bundle) {
        Builder builder = new Builder();
        List<Integer> supportedChannels =
                toIntList(requireNonNull(bundle.getIntArray(KEY_SUPPORTED_CHANNELS)));
        builder.setMinPhyVersionSupported(
                        FiraProtocolVersion.fromString(bundle.getString(KEY_MIN_PHY_VERSION)))
                .setMaxPhyVersionSupported(
                        FiraProtocolVersion.fromString(bundle.getString(KEY_MAX_PHY_VERSION)))
                .setMinMacVersionSupported(
                        FiraProtocolVersion.fromString(bundle.getString(KEY_MIN_MAC_VERSION)))
                .setMaxMacVersionSupported(
                        FiraProtocolVersion.fromString(bundle.getString(KEY_MAX_MAC_VERSION)))
                .setSupportedChannels(supportedChannels)
                .setAoaCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_AOA_CAPABILITIES), AoaCapabilityFlag.values()))
                .setDeviceRoleCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_DEVICE_ROLE_CAPABILITIES),
                                DeviceRoleCapabilityFlag.values()))
                .hasBlockStridingSupport(bundle.getBoolean(KEY_BLOCK_STRIDING_SUPPORT))
                .hasHoppingPreferenceSupport(bundle.getBoolean(KEY_HOPPING_PREFERENCE_SUPPORT))
                .hasExtendedMacAddressSupport(bundle.getBoolean(KEY_EXTENDED_MAC_ADDRESS_SUPPORT))
                .hasNonDeferredModeSupport(bundle.getBoolean(KEY_NON_DEFERRED_MODE_SUPPORT))
                .hasInitiationTimeSupport(bundle.getBoolean(KEY_INITIATION_TIME_SUPPORT))
                .setMinRangingIntervalSupported(bundle.getInt(KEY_MIN_RANGING_INTERVAL, -1))
                .setMinSlotDurationSupportedUs(bundle.getInt(KEY_MIN_SLOT_DURATION, -1))
                .setMultiNodeCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_MULTI_NODE_CAPABILITIES),
                                MultiNodeCapabilityFlag.values()))
                .setRangingTimeStructCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_RANGING_TIME_STRUCT_CAPABILITIES),
                                RangingTimeStructCapabilitiesFlag.values()))
                .setSchedulingModeCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_SCHEDULING_MODE_CAPABILITIES),
                                SchedulingModeCapabilitiesFlag.values()))
                .setCcConstraintLengthCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_CC_CONSTRAINT_LENGTH_CAPABILITIES),
                                CcConstraintLengthCapabilitiesFlag.values()))
                .setPrfCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_PRF_CAPABILITIES), PrfCapabilityFlag.values()))
                .setRangingRoundCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_RANGING_ROUND_CAPABILITIES),
                                RangingRoundCapabilityFlag.values()))
                .setRframeCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_RFRAME_CAPABILITIES),
                                RframeCapabilityFlag.values()))
                .setStsCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_STS_CAPABILITIES), StsCapabilityFlag.values()))
                .setPsduDataRateCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_PSDU_DATA_RATE_CAPABILITIES),
                                PsduDataRateCapabilityFlag.values()))
                .setBprfParameterSetCapabilities(
                        FlagEnum.toEnumSet(
                                bundle.getInt(KEY_BPRF_PARAMETER_SET_CAPABILITIES),
                                BprfParameterSetCapabilityFlag.values()))
                .setMaxMessageSize(bundle.getInt(KEY_MAX_MESSAGE_SIZE))
                .setMaxDataPacketPayloadSize(bundle.getInt(KEY_MAX_DATA_PACKET_PAYLOAD_SIZE))
                .setHprfParameterSetCapabilities(
                        FlagEnum.longToEnumSet(
                                bundle.getLong(KEY_HPRF_PARAMETER_SET_CAPABILITIES),
                                HprfParameterSetCapabilityFlag.values()));
        // Newer params need to be backward compatible with existing devices.
        if (bundle.containsKey(KEY_RANGE_DATA_NTF_CONFIG_CAPABILITIES)) {
            builder.setRangeDataNtfConfigCapabilities(
                    FlagEnum.toEnumSet(
                            bundle.getInt(KEY_RANGE_DATA_NTF_CONFIG_CAPABILITIES),
                            RangeDataNtfConfigCapabilityFlag.values()));
        }
        if (bundle.containsKey(KEY_RSSI_REPORTING_SUPPORT)) {
            builder.hasRssiReportingSupport(bundle.getBoolean(KEY_RSSI_REPORTING_SUPPORT));
        }
        if (bundle.containsKey(KEY_DIAGNOSTICS_SUPPORT)) {
            builder.hasDiagnosticsSupport(bundle.getBoolean(KEY_DIAGNOSTICS_SUPPORT));
        }
        if (bundle.containsKey(KEY_MAX_RANGING_SESSION_NUMBER)) {
            builder.setMaxRangingSessionNumberSupported(
                    bundle.getInt(KEY_MAX_RANGING_SESSION_NUMBER));
        }
        return builder;
    }

    /** Builder */
    public static class Builder {
        // Set all default protocol version to FiRa 1.1
        private FiraProtocolVersion mMinPhyVersionSupported = new FiraProtocolVersion(1, 1);
        private FiraProtocolVersion mMaxPhyVersionSupported = new FiraProtocolVersion(1, 1);
        private FiraProtocolVersion mMinMacVersionSupported = new FiraProtocolVersion(1, 1);
        private FiraProtocolVersion mMaxMacVersionSupported = new FiraProtocolVersion(1, 1);

        private List<Integer> mSupportedChannels = new ArrayList<>();

        private EnumSet<AoaCapabilityFlag> mAoaCapabilities =
                EnumSet.noneOf(AoaCapabilityFlag.class);

        // Controller-intiator, Cotrolee-responder are mandatory.
        private EnumSet<DeviceRoleCapabilityFlag> mDeviceRoleCapabilities =
                EnumSet.of(
                        DeviceRoleCapabilityFlag.HAS_CONTROLLER_INITIATOR_SUPPORT,
                        DeviceRoleCapabilityFlag.HAS_CONTROLEE_RESPONDER_SUPPORT);

        // Enable/Disable ntf config is mandatory.
        private EnumSet<RangeDataNtfConfigCapabilityFlag> mRangeDataNtfConfigCapabilities =
                EnumSet.noneOf(RangeDataNtfConfigCapabilityFlag.class);

        private boolean mHasBlockStridingSupport = false;

        private boolean mHasHoppingPreferenceSupport = false;

        private boolean mHasExtendedMacAddressSupport = false;

        private boolean mHasNonDeferredModeSupport = false;

        private boolean mHasInitiationTimeSupport = false;

        private boolean mHasRssiReportingSupport = false;

        private boolean mHasDiagnosticsSupport = false;

        private int mMinRangingInterval = -1;

        private int mMinSlotDurationUs = -1;

        private int mMaxRangingSessionNumber = DEFAULT_MAX_RANGING_SESSIONS_NUMBER;

        private int mUciVersion = 1;

        // Unicast support is mandatory
        private EnumSet<MultiNodeCapabilityFlag> mMultiNodeCapabilities =
                EnumSet.of(MultiNodeCapabilityFlag.HAS_UNICAST_SUPPORT);

        private EnumSet<RangingTimeStructCapabilitiesFlag> mRangingTimeStructCapabilities =
                EnumSet.of(
                        RangingTimeStructCapabilitiesFlag.HAS_INTERVAL_BASED_SCHEDULING_SUPPORT,
                        RangingTimeStructCapabilitiesFlag.HAS_BLOCK_BASED_SCHEDULING_SUPPORT);

        private EnumSet<SchedulingModeCapabilitiesFlag> mSchedulingModeCapabilities =
                EnumSet.of(
                        SchedulingModeCapabilitiesFlag.HAS_CONTENTION_BASED_RANGING_SUPPORT,
                        SchedulingModeCapabilitiesFlag.HAS_TIME_SCHEDULED_RANGING_SUPPORT);

        private EnumSet<CcConstraintLengthCapabilitiesFlag> mCcConstraintLengthCapabilities =
                EnumSet.of(
                        CcConstraintLengthCapabilitiesFlag.HAS_CONSTRAINT_LENGTH_3_SUPPORT,
                        CcConstraintLengthCapabilitiesFlag.HAS_CONSTRAINT_LENGTH_7_SUPPORT);

        // BPRF mode is mandatory
        private EnumSet<PrfCapabilityFlag> mPrfCapabilities =
                EnumSet.of(PrfCapabilityFlag.HAS_BPRF_SUPPORT);

        // DS-TWR is mandatory
        private EnumSet<RangingRoundCapabilityFlag> mRangingRoundCapabilities =
                EnumSet.of(RangingRoundCapabilityFlag.HAS_DS_TWR_SUPPORT);

        // SP3 RFrame is mandatory
        private EnumSet<RframeCapabilityFlag> mRframeCapabilities =
                EnumSet.of(RframeCapabilityFlag.HAS_SP3_RFRAME_SUPPORT);

        // STATIC STS is mandatory
        private EnumSet<StsCapabilityFlag> mStsCapabilities =
                EnumSet.of(StsCapabilityFlag.HAS_STATIC_STS_SUPPORT);

        // 6.81Mb/s PSDU data rate is mandatory
        private EnumSet<PsduDataRateCapabilityFlag> mPsduDataRateCapabilities =
                EnumSet.of(PsduDataRateCapabilityFlag.HAS_6M81_SUPPORT);

        private EnumSet<BprfParameterSetCapabilityFlag> mBprfParameterSetCapabilities =
                EnumSet.noneOf(BprfParameterSetCapabilityFlag.class);

        private EnumSet<HprfParameterSetCapabilityFlag> mHprfParameterSetCapabilities =
                EnumSet.noneOf(HprfParameterSetCapabilityFlag.class);

        private Integer mMaxMessageSize = 0;

        private Integer mMaxDataPacketPayloadSize = 0;

        // Default to Controlee
        private int mDeviceType = RANGING_DEVICE_TYPE_CONTROLEE;

        // Default to no suspend ranging support
        private boolean mSuspendRangingSupport = false;

        // Default to 256 bits key length not supported
        private int mSessionKeyLength = 0;

        //Default to 0 i.e., DT tag role not supported.
        private int mDtTagMaxActiveRr = 0;

        //Default is false. i.e., no background ranging support.
        private boolean mHasBackgroundRangingSupport = false;

        // DT_TAG_BLOCK_SKIPPING.
        private boolean mHasDtTagBlockSkippingSupport = false;

        // PSDU_LENGTH_SUPPORT.
        private boolean mHasPsduLengthSupport = false;

        public Builder setMinPhyVersionSupported(
                FiraProtocolVersion version) {
            mMinPhyVersionSupported = version;
            return this;
        }

        public Builder setMaxPhyVersionSupported(
                FiraProtocolVersion version) {
            mMaxPhyVersionSupported = version;
            return this;
        }

        public Builder setMinMacVersionSupported(
                FiraProtocolVersion version) {
            mMinMacVersionSupported = version;
            return this;
        }

        public Builder setMaxMacVersionSupported(
                FiraProtocolVersion version) {
            mMaxMacVersionSupported = version;
            return this;
        }

        public Builder setSupportedChannels(
                List<Integer> supportedChannels) {
            mSupportedChannels = List.copyOf(supportedChannels);
            return this;
        }

        public Builder setAoaCapabilities(
                Collection<AoaCapabilityFlag> aoaCapabilities) {
            mAoaCapabilities.addAll(aoaCapabilities);
            return this;
        }

        public Builder setDeviceRoleCapabilities(
                Collection<DeviceRoleCapabilityFlag> deviceRoleCapabilities) {
            mDeviceRoleCapabilities.addAll(deviceRoleCapabilities);
            return this;
        }

        public Builder hasBlockStridingSupport(boolean value) {
            mHasBlockStridingSupport = value;
            return this;
        }

        public Builder hasHoppingPreferenceSupport(boolean value) {
            mHasHoppingPreferenceSupport = value;
            return this;
        }

        public Builder hasExtendedMacAddressSupport(boolean value) {
            mHasExtendedMacAddressSupport = value;
            return this;
        }

        public Builder hasNonDeferredModeSupport(boolean value) {
            mHasNonDeferredModeSupport = value;
            return this;
        }

        public Builder hasInitiationTimeSupport(boolean value) {
            mHasInitiationTimeSupport = value;
            return this;
        }

        /** Set whether rssi reporting is supported. */
        public Builder hasRssiReportingSupport(boolean value) {
            mHasRssiReportingSupport = value;
            return this;
        }

        /** Set whether diagnostics is supported. */
        public Builder hasDiagnosticsSupport(boolean value) {
            mHasDiagnosticsSupport = value;
            return this;
        }

        /**
         * Set minimum supported ranging interval
         * @param value : minimum ranging interval supported
         * @return FiraSpecificationParams builder
         */
        public Builder setMinRangingIntervalSupported(int value) {
            mMinRangingInterval = value;
            return this;
        }

        /**
         * Set minimum supported slot duration in microsecond
         * @param value : minimum slot duration supported in microsecond
         * @return FiraSpecificationParams builder
         */
        public Builder setMinSlotDurationSupportedUs(int value) {
            mMinSlotDurationUs = value;
            return this;
        }

        /**
         * Set maximum supported ranging session number
         * @param value : maximum ranging session number supported
         * @return FiraSpecificationParams builder
         */
        public Builder setMaxRangingSessionNumberSupported(int value) {
            mMaxRangingSessionNumber = value;
            return this;
        }

        public Builder setMultiNodeCapabilities(
                Collection<MultiNodeCapabilityFlag> multiNodeCapabilities) {
            mMultiNodeCapabilities.addAll(multiNodeCapabilities);
            return this;
        }

        public Builder setRangingTimeStructCapabilities(
                Collection<RangingTimeStructCapabilitiesFlag> rangingTimeStructCapabilities) {
            mRangingTimeStructCapabilities.addAll(rangingTimeStructCapabilities);
            return this;
        }

        public Builder setSchedulingModeCapabilities(
                Collection<SchedulingModeCapabilitiesFlag> schedulingModeCapabilities) {
            mSchedulingModeCapabilities.addAll(schedulingModeCapabilities);
            return this;
        }

        public Builder setCcConstraintLengthCapabilities(
                Collection<CcConstraintLengthCapabilitiesFlag> ccConstraintLengthCapabilities) {
            mCcConstraintLengthCapabilities.addAll(ccConstraintLengthCapabilities);
            return this;
        }

        public Builder setPrfCapabilities(
                Collection<PrfCapabilityFlag> prfCapabilities) {
            mPrfCapabilities.addAll(prfCapabilities);
            return this;
        }

        public Builder setRangingRoundCapabilities(
                Collection<RangingRoundCapabilityFlag> rangingRoundCapabilities) {
            mRangingRoundCapabilities.addAll(rangingRoundCapabilities);
            return this;
        }

        public Builder setRframeCapabilities(
                Collection<RframeCapabilityFlag> rframeCapabilities) {
            mRframeCapabilities.addAll(rframeCapabilities);
            return this;
        }

        public Builder setStsCapabilities(
                Collection<StsCapabilityFlag> stsCapabilities) {
            mStsCapabilities.addAll(stsCapabilities);
            return this;
        }

        public Builder setPsduDataRateCapabilities(
                Collection<PsduDataRateCapabilityFlag> psduDataRateCapabilities) {
            mPsduDataRateCapabilities.addAll(psduDataRateCapabilities);
            return this;
        }

        public Builder setBprfParameterSetCapabilities(
                Collection<BprfParameterSetCapabilityFlag> bprfParameterSetCapabilities) {
            mBprfParameterSetCapabilities.addAll(bprfParameterSetCapabilities);
            return this;
        }

        public Builder setHprfParameterSetCapabilities(
                Collection<HprfParameterSetCapabilityFlag> hprfParameterSetCapabilities) {
            mHprfParameterSetCapabilities.addAll(hprfParameterSetCapabilities);
            return this;
        }

        public Builder setRangeDataNtfConfigCapabilities(
                Collection<RangeDataNtfConfigCapabilityFlag> rangeDataNtfConfigCapabilities) {
            mRangeDataNtfConfigCapabilities.addAll(rangeDataNtfConfigCapabilities);
            return this;
        }

        public Builder setMaxMessageSize(Integer value) {
            mMaxMessageSize = value;
            return this;
        }

        public Builder setMaxDataPacketPayloadSize(Integer value) {
            mMaxDataPacketPayloadSize = value;
            return this;
        }

        public Builder setDeviceType(Integer value) {
            mDeviceType = value;
            return this;

        }

        public Builder setSuspendRangingSupport(Boolean value) {
            mSuspendRangingSupport = value;
            return this;
        }

        public Builder setSessionKeyLength(Integer value) {
            mSessionKeyLength = value;
            return this;
        }

        public Builder setDtTagMaxActiveRr(int value) {
            mDtTagMaxActiveRr = value;
            return this;
        }

        public Builder setBackgroundRangingSupport(boolean value) {
            mHasBackgroundRangingSupport = value;
            return this;
        }

        public Builder setDtTagBlockSkippingSupport(boolean value) {
            mHasDtTagBlockSkippingSupport = value;
            return this;
        }

        public Builder setPsduLengthSupport(boolean value) {
            mHasPsduLengthSupport = value;
            return this;
        }

        public Builder setUciVersionSupported(
                int uciVersion) {
            mUciVersion = uciVersion;
            return this;
        }

        public Builder() {}

        public Builder(@NonNull FiraSpecificationParams params) {
            mMinPhyVersionSupported = params.mMinPhyVersionSupported;
            mMaxPhyVersionSupported = params.mMaxPhyVersionSupported;
            mMinMacVersionSupported = params.mMinMacVersionSupported;
            mMaxMacVersionSupported = params.mMaxMacVersionSupported;
            mSupportedChannels = params.mSupportedChannels;
            mAoaCapabilities = params.mAoaCapabilities;
            mDeviceRoleCapabilities = params.mDeviceRoleCapabilities;
            mHasBlockStridingSupport = params.mHasBlockStridingSupport;
            mHasHoppingPreferenceSupport = params.mHasHoppingPreferenceSupport;
            mHasExtendedMacAddressSupport = params.mHasExtendedMacAddressSupport;
            mHasNonDeferredModeSupport = params.mHasNonDeferredModeSupport;
            mHasInitiationTimeSupport = params.mHasInitiationTimeSupport;
            mHasRssiReportingSupport = params.mHasRssiReportingSupport;
            mHasDiagnosticsSupport = params.mHasDiagnosticsSupport;
            mMinRangingInterval = params.mMinRangingInterval;
            mMinSlotDurationUs = params.mMinSlotDurationUs;
            mMaxRangingSessionNumber = params.mMaxRangingSessionNumber;
            mMultiNodeCapabilities = params.mMultiNodeCapabilities;
            mRangingTimeStructCapabilities = params.mRangingTimeStructCapabilities;
            mSchedulingModeCapabilities = params.mSchedulingModeCapabilities;
            mCcConstraintLengthCapabilities = params.mCcConstraintLengthCapabilities;
            mPrfCapabilities = params.mPrfCapabilities;
            mRangingRoundCapabilities = params.mRangingRoundCapabilities;
            mRframeCapabilities = params.mRframeCapabilities;
            mStsCapabilities = params.mStsCapabilities;
            mPsduDataRateCapabilities = params.mPsduDataRateCapabilities;
            mBprfParameterSetCapabilities = params.mBprfParameterSetCapabilities;
            mHprfParameterSetCapabilities = params.mHprfParameterSetCapabilities;
            mMaxMessageSize = params.mMaxMessageSize;
            mMaxDataPacketPayloadSize = params.mMaxDataPacketPayloadSize;
            mRangeDataNtfConfigCapabilities = params.mRangeDataNtfConfigCapabilities;
            mDeviceType = params.mDeviceType;
            mSuspendRangingSupport = params.mSuspendRangingSupport;
            mSessionKeyLength = params.mSessionKeyLength;
            mDtTagMaxActiveRr = params.mDtTagMaxActiveRr;
            mHasBackgroundRangingSupport = params.mHasBackgroundRangingSupport;
            mHasDtTagBlockSkippingSupport = params.mHasDtTagBlockSkippingSupport;
            mHasPsduLengthSupport = params.mHasPsduLengthSupport;
        }

        public FiraSpecificationParams build() {
            return new FiraSpecificationParams(
                    mMinPhyVersionSupported,
                    mMaxPhyVersionSupported,
                    mMinMacVersionSupported,
                    mMaxMacVersionSupported,
                    mSupportedChannels,
                    mAoaCapabilities,
                    mDeviceRoleCapabilities,
                    mHasBlockStridingSupport,
                    mHasHoppingPreferenceSupport,
                    mHasExtendedMacAddressSupport,
                    mHasNonDeferredModeSupport,
                    mHasInitiationTimeSupport,
                    mHasRssiReportingSupport,
                    mHasDiagnosticsSupport,
                    mMinRangingInterval,
                    mMinSlotDurationUs,
                    mMaxRangingSessionNumber,
                    mMultiNodeCapabilities,
                    mRangingTimeStructCapabilities,
                    mSchedulingModeCapabilities,
                    mCcConstraintLengthCapabilities,
                    mPrfCapabilities,
                    mRangingRoundCapabilities,
                    mRframeCapabilities,
                    mStsCapabilities,
                    mPsduDataRateCapabilities,
                    mBprfParameterSetCapabilities,
                    mHprfParameterSetCapabilities,
                    mMaxMessageSize,
                    mMaxDataPacketPayloadSize,
                    mRangeDataNtfConfigCapabilities,
                    mDeviceType,
                    mSuspendRangingSupport,
                    mSessionKeyLength,
                    mDtTagMaxActiveRr,
                    mHasBackgroundRangingSupport,
                    mHasDtTagBlockSkippingSupport,
                    mHasPsduLengthSupport,
                    mUciVersion);
        }
    }
}
